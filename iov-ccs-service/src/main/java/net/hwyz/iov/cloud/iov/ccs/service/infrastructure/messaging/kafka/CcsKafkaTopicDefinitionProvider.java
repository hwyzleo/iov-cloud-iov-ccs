package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * CCS 生产 Kafka Topic 定义提供者（IOV-CCS-DSN-CR-003）
 * <p>
 * 向 FW-KAFKA 声明 CCS 作为生产者的全部 Topic（SIM 状态变更 / 车卡绑定状态变更），
 * 由框架统一完成存在性检查、幂等创建、后台重试与状态传播。
 * <p>
 * Topic 名称、分区数、副本数与 Topic 参数统一来自 {@link CcsKafkaTopicProperties}，
 * 与发布器 {@code CardStatusEventPublisherImpl} 引用同一配置入口，业务代码不散落 Topic 字符串；
 * 仅声明 CCS 所有的生产 Topic，不声明任何消费 Topic。
 *
 * @author hwyz_leo
 */
@Component
public class CcsKafkaTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final CcsKafkaTopicProperties properties;

    public CcsKafkaTopicDefinitionProvider(CcsKafkaTopicProperties properties) {
        this.properties = properties;
    }

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        return List.of(
                new KafkaTopicDefinition(
                        properties.getSimStatusChanged().getName(),
                        properties.getPartitions(),
                        properties.getReplicationFactor(),
                        properties.getConfigs()),
                new KafkaTopicDefinition(
                        properties.getVehicleSimBindingStatusChanged().getName(),
                        properties.getPartitions(),
                        properties.getReplicationFactor(),
                        properties.getConfigs()));
    }
}
