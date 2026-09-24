package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicCatalog;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka.KafkaTopicConfigAuditor.AuditResult;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka.KafkaTopicConfigAuditor.TopicConfigDiff;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka Topic 配置差异审计单元测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 验证分区数/副本数/Topic 参数差异计算、匹配时无差异、只审计不改动。
 *
 * @author hwyz_leo
 */
@DisplayName("KafkaTopicConfigAuditor 测试")
class KafkaTopicConfigAuditorTest {

    private static final String SIM_TOPIC = "ccs.sim-status.changed";

    private KafkaTopicDefinition definition(int partitions, short replication, Map<String, String> configs) {
        return new KafkaTopicDefinition(SIM_TOPIC, partitions, replication, configs);
    }

    private KafkaTopicCatalog catalog(KafkaTopicDefinition... definitions) {
        KafkaTopicCatalog catalog = mock(KafkaTopicCatalog.class);
        when(catalog.definitions()).thenReturn(List.of(definitions));
        return catalog;
    }

    private TopicDescription description(int partitions, short replication) {
        Node node = new Node(0, "localhost", 9092);
        List<Node> replicas = new ArrayList<>();
        for (int i = 0; i < replication; i++) {
            replicas.add(new Node(i, "localhost", 9092 + i));
        }
        List<TopicPartitionInfo> partitionInfos = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            partitionInfos.add(new TopicPartitionInfo(i, node, replicas, replicas));
        }
        return new TopicDescription(SIM_TOPIC, false, partitionInfos);
    }

    private Admin adminWithTopics(Map<String, TopicDescription> descriptions) {
        Admin admin = mock(Admin.class);
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        Map<String, KafkaFuture<TopicDescription>> futures = new HashMap<>();
        descriptions.forEach((name, desc) -> futures.put(name, KafkaFuture.completedFuture(desc)));
        when(result.values()).thenReturn(futures);
        when(admin.describeTopics(any(Collection.class))).thenReturn(result);
        return admin;
    }

    private void mockEffectiveConfigs(Admin admin, Map<String, String> effectiveByTopic) {
        DescribeConfigsResult result = mock(DescribeConfigsResult.class);
        Map<ConfigResource, KafkaFuture<Config>> futures = new HashMap<>();
        effectiveByTopic.forEach((name, retentionMs) -> {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            futures.put(resource, KafkaFuture.completedFuture(
                    new Config(List.of(new ConfigEntry("retention.ms", retentionMs)))));
        });
        when(result.values()).thenReturn(futures);
        when(admin.describeConfigs(any())).thenReturn(result);
    }

    private KafkaTopicConfigAuditor auditor(Admin admin, KafkaTopicCatalog catalog) {
        return new KafkaTopicConfigAuditor(admin, catalog);
    }

    @Test
    @DisplayName("分区数/副本数/配置全部匹配时无差异")
    void matchingConfig_noDiff() {
        Admin admin = adminWithTopics(Map.of(SIM_TOPIC, description(3, (short) 3)));
        mockEffectiveConfigs(admin, Map.of(SIM_TOPIC, "604800000"));
        KafkaTopicConfigAuditor auditor = auditor(admin,
                catalog(definition(3, (short) 3, Map.of("retention.ms", "604800000"))));

        AuditResult result = auditor.audit();

        assertTrue(result.diffs().isEmpty(), "期望无差异，实际差异：" + result.diffs());
        assertTrue(result.warnings().isEmpty(), "期望无警告，实际警告：" + result.warnings());
    }

    @Test
    @DisplayName("分区数不一致计算差异明细")
    void partitionMismatch_diffPresent() {
        Admin admin = adminWithTopics(Map.of(SIM_TOPIC, description(1, (short) 3)));
        KafkaTopicConfigAuditor auditor = auditor(admin, catalog(definition(3, (short) 3, Map.of())));

        AuditResult result = auditor.audit();

        List<TopicConfigDiff> diffs = result.diffs();
        assertEquals(1, diffs.size());
        assertEquals("分区数", diffs.get(0).field());
        assertEquals("3", diffs.get(0).expected());
        assertEquals("1", diffs.get(0).actual());
    }

    @Test
    @DisplayName("副本数不一致计算差异明细")
    void replicationMismatch_diffPresent() {
        Admin admin = adminWithTopics(Map.of(SIM_TOPIC, description(3, (short) 1)));
        KafkaTopicConfigAuditor auditor = auditor(admin, catalog(definition(3, (short) 3, Map.of())));

        AuditResult result = auditor.audit();

        List<TopicConfigDiff> diffs = result.diffs();
        assertEquals(1, diffs.size());
        assertEquals("副本数", diffs.get(0).field());
        assertEquals("3", diffs.get(0).expected());
        assertEquals("1", diffs.get(0).actual());
    }

    @Test
    @DisplayName("Topic 参数不一致计算差异明细")
    void configMismatch_diffPresent() {
        Admin admin = adminWithTopics(Map.of(SIM_TOPIC, description(3, (short) 3)));
        mockEffectiveConfigs(admin, Map.of(SIM_TOPIC, "3600000"));
        KafkaTopicConfigAuditor auditor = auditor(admin,
                catalog(definition(3, (short) 3, Map.of("retention.ms", "604800000"))));

        AuditResult result = auditor.audit();

        List<TopicConfigDiff> diffs = result.diffs();
        assertEquals(1, diffs.size());
        assertEquals("retention.ms", diffs.get(0).field());
        assertEquals("604800000", diffs.get(0).expected());
        assertEquals("3600000", diffs.get(0).actual());
    }

    @Test
    @DisplayName("未声明期望 Topic 参数时不调用 describeConfigs（只审计声明项）")
    void noExpectedConfigs_describeConfigsNotCalled() {
        Admin admin = adminWithTopics(Map.of(SIM_TOPIC, description(3, (short) 3)));
        KafkaTopicConfigAuditor auditor = auditor(admin, catalog(definition(3, (short) 3, Map.of())));

        AuditResult result = auditor.audit();

        assertTrue(result.diffs().isEmpty());
        verify(admin, never()).describeConfigs(any());
    }

    @Test
    @DisplayName("Topic 不存在时记录可诊断警告")
    void topicMissing_warning() throws Exception {
        Admin admin = mock(Admin.class);
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        KafkaFuture<TopicDescription> failed = mock(KafkaFuture.class);
        when(failed.get(anyLong(), any())).thenThrow(
                new ExecutionException(new UnknownTopicOrPartitionException("topic not found")));
        when(result.values()).thenReturn(Map.of(SIM_TOPIC, failed));
        when(admin.describeTopics(any(Collection.class))).thenReturn(result);

        KafkaTopicConfigAuditor auditor = auditor(admin, catalog(definition(3, (short) 3, Map.of())));
        AuditResult result2 = auditor.audit();

        assertTrue(result2.diffs().isEmpty());
        assertTrue(result2.warnings().stream().anyMatch(w -> w.contains("Topic 不存在或刚被删除/创建中")));
    }

    @Test
    @DisplayName("describe 异常时记录审计失败警告")
    void describeFailure_warning() {
        Admin admin = mock(Admin.class);
        when(admin.describeTopics(any(Collection.class))).thenThrow(new RuntimeException("connection refused"));
        KafkaTopicConfigAuditor auditor = auditor(admin, catalog(definition(3, (short) 3, Map.of())));

        AuditResult result = auditor.audit();

        assertTrue(result.diffs().isEmpty());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("审计失败")));
    }
}
