package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicCatalog;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicsReadyEvent;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Kafka Topic 配置差异审计（IOV-CCS-DSN-CR-003）
 * <p>
 * Topic Provisioning 首次就绪后，对照期望配置审计既有 Topic：
 * <ul>
 *   <li>分区数、副本数与期望不一致 → 输出告警与差异明细</li>
 *   <li>期望的 Topic 关键配置（retention/cleanup 等）与 Broker 实际生效值不一致 → 输出告警与差异明细</li>
 *   <li>只告警：不删除、不重建、不扩/缩分区、不 alterConfigs；有损或高风险变更由运维流程处理</li>
 * </ul>
 * 期望值来自 {@link KafkaTopicCatalog}（合并后 SSOT），即 {@code CcsKafkaTopicProperties}。
 * {@link #audit()} 返回结构化差异结果便于测试与排障，监听器负责输出告警日志。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTopicConfigAuditor {

    private static final long ADMIN_TIMEOUT_SECONDS = 5;

    private final Admin admin;
    private final KafkaTopicCatalog catalog;

    /**
     * Topic Provisioning 首次就绪后触发审计。
     */
    @EventListener
    public void onTopicsReady(KafkaTopicsReadyEvent event) {
        AuditResult result = audit();
        for (TopicConfigDiff diff : result.diffs()) {
            log.warn("Kafka Topic 配置差异：{}（只告警不改动，请由运维流程处理）", diff);
        }
        for (String warning : result.warnings()) {
            log.warn("Kafka Topic 配置审计：{}", warning);
        }
    }

    /**
     * 审计全部已声明生产 Topic 与期望配置的差异，仅计算不修改任何 Topic。
     *
     * @return 结构化审计结果：差异明细 + 审计警告
     */
    public AuditResult audit() {
        Collection<KafkaTopicDefinition> definitions = catalog.definitions();
        if (definitions.isEmpty()) {
            return AuditResult.empty();
        }
        Map<String, KafkaTopicDefinition> byName = definitions.stream()
                .collect(Collectors.toMap(KafkaTopicDefinition::name, d -> d));
        List<TopicConfigDiff> diffs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try {
            DescribeTopicsResult describeResult = admin.describeTopics(byName.keySet());
            for (Map.Entry<String, KafkaFuture<TopicDescription>> entry : describeResult.values().entrySet()) {
                String name = entry.getKey();
                KafkaTopicDefinition expected = byName.get(name);
                try {
                    TopicDescription actual = entry.getValue().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    diffs.addAll(auditTopic(expected, actual));
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                        warnings.add("Topic 不存在或刚被删除/创建中，topic=" + name);
                    } else {
                        warnings.add("读取 Topic 元数据失败，topic=" + name + "，error=" + causeMessage(e));
                    }
                } catch (TimeoutException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    warnings.add("读取 Topic 元数据超时，topic=" + name);
                }
            }
            diffs.addAll(auditConfigs(byName, warnings));
        } catch (Exception e) {
            warnings.add("审计失败：" + causeMessage(e));
        }
        return new AuditResult(diffs, warnings);
    }

    private List<TopicConfigDiff> auditTopic(KafkaTopicDefinition expected, TopicDescription actual) {
        List<TopicConfigDiff> diffs = new ArrayList<>();
        int actualPartitions = actual.partitions().size();
        if (actualPartitions != expected.partitions()) {
            diffs.add(new TopicConfigDiff(expected.name(), "分区数",
                    String.valueOf(expected.partitions()), String.valueOf(actualPartitions)));
        }
        short actualReplication = effectiveReplication(actual);
        if (actualReplication != expected.replicationFactor()) {
            diffs.add(new TopicConfigDiff(expected.name(), "副本数",
                    String.valueOf(expected.replicationFactor()), String.valueOf(actualReplication)));
        }
        return diffs;
    }

    private List<TopicConfigDiff> auditConfigs(Map<String, KafkaTopicDefinition> byName, List<String> warnings) {
        List<TopicConfigDiff> diffs = new ArrayList<>();
        List<String> namesWithExpectedConfigs = byName.values().stream()
                .filter(d -> !d.configs().isEmpty())
                .map(KafkaTopicDefinition::name)
                .toList();
        if (namesWithExpectedConfigs.isEmpty()) {
            return diffs;
        }
        List<ConfigResource> resources = namesWithExpectedConfigs.stream()
                .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                .toList();
        try {
            DescribeConfigsResult result = admin.describeConfigs(resources);
            for (ConfigResource resource : resources) {
                KafkaTopicDefinition expected = byName.get(resource.name());
                try {
                    Config actual = result.values().get(resource).get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Map<String, String> actualByKey = new HashMap<>();
                    for (ConfigEntry entry : actual.entries()) {
                        actualByKey.put(entry.name(), entry.value());
                    }
                    expected.configs().forEach((key, expectedValue) -> {
                        String actualValue = actualByKey.get(key);
                        if (!expectedValue.equals(actualValue)) {
                            diffs.add(new TopicConfigDiff(resource.name(), key,
                                    expectedValue, String.valueOf(actualValue)));
                        }
                    });
                } catch (ExecutionException e) {
                    warnings.add("读取 Topic 配置失败，topic=" + resource.name() + "，error=" + causeMessage(e));
                } catch (TimeoutException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    warnings.add("读取 Topic 配置超时，topic=" + resource.name());
                }
            }
        } catch (Exception e) {
            warnings.add("审计 Topic 参数失败：" + causeMessage(e));
        }
        return diffs;
    }

    /**
     * 取各分区副本数的最大值作为 Topic 有效副本数（正常场景各分区一致）。
     */
    private short effectiveReplication(TopicDescription description) {
        return description.partitions().stream()
                .map(p -> (short) p.replicas().size())
                .max(Short::compareTo)
                .orElse((short) 0);
    }

    private String causeMessage(Throwable ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /**
     * 单条 Topic 配置差异。
     *
     * @param topic     Topic 名称
     * @param field     差异字段（分区数 / 副本数 / 配置键）
     * @param expected  期望值
     * @param actual    实际生效值
     */
    public record TopicConfigDiff(String topic, String field, String expected, String actual) {

        @Override
        public String toString() {
            return "topic=" + topic + "，" + field + " 期望=" + expected + "，实际=" + actual;
        }
    }

    /**
     * 审计结果：差异明细 + 审计警告（Topic 缺失、元数据读取失败等）。
     */
    public record AuditResult(List<TopicConfigDiff> diffs, List<String> warnings) {

        public AuditResult {
            diffs = List.copyOf(diffs);
            warnings = List.copyOf(warnings);
        }

        public static AuditResult empty() {
            return new AuditResult(List.of(), List.of());
        }
    }
}
