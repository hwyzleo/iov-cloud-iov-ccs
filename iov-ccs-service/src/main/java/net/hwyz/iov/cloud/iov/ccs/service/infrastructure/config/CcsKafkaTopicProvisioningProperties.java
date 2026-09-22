package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CCS Kafka Topic 声明环境参数
 * <p>
 * 由 {@code CcsKafkaTopicDefinitionProvider} 读取并构造 KafkaTopicDefinition；
 * 分区数、副本数等环境参数通过 Nacos 配置绑定覆盖。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ccs.kafka.topic-provisioning")
public class CcsKafkaTopicProvisioningProperties {

    /**
     * Topic 分区数
     */
    private int partitions = 3;

    /**
     * Topic 副本数（默认 1，适配单节点 broker；生产多副本环境经 Nacos 覆盖）
     */
    private short replicationFactor = 1;
}
