package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Kafka Topic 就绪指示器（IOV-CCS-DSN-CR-003）
 * <p>
 * 基于 FW-KAFKA {@link KafkaTopicProvisioningStatus} 将 Topic 初始化结果接入 Spring readiness：
 * <ul>
 *   <li>READY → UP：全部生产 Topic 已存在或创建成功，事件发布通道可用</li>
 *   <li>NOT_READY → DOWN：存在缺失 Topic 或最近一次检查失败，readiness 阻断（不启动事件发布通道）</li>
 *   <li>DISABLED / 未启用 Provisioning → UP：无 Topic 初始化约束，不做门禁</li>
 * </ul>
 * 缺失清单、最近失败原因与下次重试时间随 health 明细输出，便于排障。
 * 仅输出状态与告警，不创建/删除/修改任何 Topic。
 *
 * @author hwyz_leo
 */
@Component
public class KafkaTopicReadinessIndicator implements HealthIndicator {

    private final ObjectProvider<KafkaTopicProvisioningStatus> statusProvider;

    public KafkaTopicReadinessIndicator(ObjectProvider<KafkaTopicProvisioningStatus> statusProvider) {
        this.statusProvider = statusProvider;
    }

    @Override
    public Health health() {
        KafkaTopicProvisioningStatus status = statusProvider.getIfAvailable();
        if (status == null) {
            return Health.up().withDetail("provisioning", "disabled").build();
        }
        KafkaTopicProvisioningStatus.State state = status.state();
        if (state == KafkaTopicProvisioningStatus.State.READY) {
            return Health.up().withDetail("state", "READY").build();
        }
        if (state == KafkaTopicProvisioningStatus.State.DISABLED) {
            return Health.up().withDetail("state", "DISABLED").build();
        }
        Health.Builder builder = Health.down()
                .withDetail("state", "NOT_READY")
                .withDetail("missingTopics", status.missingTopics());
        status.nextRetryAt().ifPresent(next -> builder.withDetail("nextRetryAt", next.toString()));
        status.lastFailure().ifPresent(builder::withException);
        return builder.build();
    }
}
