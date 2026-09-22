package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProvisioningProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CCS 下游 Kafka Topic 定义提供者
 * <p>
 * 向 FW-KAFKA 声明 CCS 作为生产者的全部 Topic（SIM 状态变更 / 车卡绑定状态变更），
 * 由框架统一完成 Catalog 合并、存在性检查、幂等创建、后台重试与状态传播。
 * <p>
 * Topic 名称复用 {@code kafka.topic.*} 配置（与发布器 {@code CardStatusEventPublisherImpl}
 * 保持一致），分区数、副本数通过 {@link CcsKafkaTopicProvisioningProperties} 环境参数注入。
 *
 * @author hwyz_leo
 */
@Component
public class CcsKafkaTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final CcsKafkaTopicProvisioningProperties properties;
    private final String simStatusTopic;
    private final String cardBindingStatusTopic;

    public CcsKafkaTopicDefinitionProvider(
            CcsKafkaTopicProvisioningProperties properties,
            @Value("${kafka.topic.sim-status:ccs-sim-status-changed}") String simStatusTopic,
            @Value("${kafka.topic.card-binding-status:card-binding-status-changed}") String cardBindingStatusTopic) {
        this.properties = properties;
        this.simStatusTopic = simStatusTopic;
        this.cardBindingStatusTopic = cardBindingStatusTopic;
    }

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        Set<String> names = new LinkedHashSet<>(List.of(simStatusTopic, cardBindingStatusTopic));
        return names.stream()
                .map(name -> new KafkaTopicDefinition(
                        name, properties.getPartitions(), properties.getReplicationFactor()))
                .toList();
    }
}
