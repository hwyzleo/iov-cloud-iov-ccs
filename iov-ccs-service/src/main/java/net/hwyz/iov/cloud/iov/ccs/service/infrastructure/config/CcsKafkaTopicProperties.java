package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * CCS Kafka Topic 单一配置入口（IOV-CCS-DSN-CR-003）
 * <p>
 * 以 Kafka Topic 目录为契约基准，统一 IOV-CCS 两个生产 Topic 的名称与容量参数：
 * <ul>
 *   <li>{@code simStatusChanged} → {@code ccs.sim-status.changed}（SIM 运营态/实名状态变更）</li>
 *   <li>{@code vehicleSimBindingStatusChanged} → {@code ccs.vehicle-sim-binding-status.changed}（VIN↔ICCID 绑定状态变更）</li>
 * </ul>
 * 分区数、副本数、retention/cleanup 等 Topic 参数均环境化（Nacos 配置覆盖），不硬编码；
 * Producer、测试、监控标签与 TopicInitializer 统一引用本配置，业务代码不散落 Topic 字符串。
 *
 * @author hwyz_leo
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ccs.kafka.topic")
public class CcsKafkaTopicProperties {

    /**
     * SIM 状态变更事件 Topic 绑定
     */
    @Valid
    private TopicBinding simStatusChanged = new TopicBinding("ccs.sim-status.changed");

    /**
     * 车卡绑定状态变更事件 Topic 绑定
     */
    @Valid
    private TopicBinding vehicleSimBindingStatusChanged = new TopicBinding("ccs.vehicle-sim-binding-status.changed");

    /**
     * Topic 分区数（默认 3，生产环境经 Nacos 覆盖）
     */
    @Min(1)
    private int partitions = 3;

    /**
     * Topic 副本数（默认 1，适配单节点 broker；生产多副本环境经 Nacos 覆盖）
     */
    @Min(1)
    private short replicationFactor = 1;

    /**
     * Topic 级配置（如 retention.ms / cleanup.policy / compression.type），仅首次创建时生效；
     * 已存在 Topic 不执行 alterConfigs。默认空 = 使用 broker 默认配置。
     */
    private Map<String, String> configs = new HashMap<>();

    /**
     * 单个事件 Topic 绑定
     */
    @Data
    public static class TopicBinding {

        /**
         * 标准 Topic 名称（以 Kafka Topic 目录为契约基准）
         */
        @NotBlank
        private String name;

        public TopicBinding() {
        }

        public TopicBinding(String name) {
            this.name = name;
        }
    }
}
