package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Kafka Topic 就绪指示器单元测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 验证 Topic Provisioning 状态到 Spring readiness 的映射：
 * READY→UP、NOT_READY→DOWN（含缺失清单与失败明细）、DISABLED/未启用→UP 不门禁。
 *
 * @author hwyz_leo
 */
@DisplayName("KafkaTopicReadinessIndicator 测试")
class KafkaTopicReadinessIndicatorTest {

    private KafkaTopicReadinessIndicator indicator(KafkaTopicProvisioningStatus status) {
        ObjectProvider<KafkaTopicProvisioningStatus> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(status);
        return new KafkaTopicReadinessIndicator(provider);
    }

    @Test
    @DisplayName("READY → UP")
    void readyIsUp() {
        KafkaTopicProvisioningStatus status = mock(KafkaTopicProvisioningStatus.class);
        when(status.state()).thenReturn(KafkaTopicProvisioningStatus.State.READY);
        Health health = indicator(status).health();
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    @DisplayName("NOT_READY → DOWN 并输出缺失清单与失败明细")
    void notReadyIsDownWithDetails() {
        KafkaTopicProvisioningStatus status = mock(KafkaTopicProvisioningStatus.class);
        when(status.state()).thenReturn(KafkaTopicProvisioningStatus.State.NOT_READY);
        when(status.missingTopics()).thenReturn(Set.of("ccs.sim-status.changed"));
        when(status.lastFailure()).thenReturn(Optional.of(new IllegalStateException("broker unreachable")));
        when(status.nextRetryAt()).thenReturn(Optional.of(Instant.parse("2026-09-24T08:00:00Z")));

        Health health = indicator(status).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(Set.of("ccs.sim-status.changed"), health.getDetails().get("missingTopics"));
        assertTrue(health.getDetails().get("error") != null || health.getDetails().toString().contains("broker unreachable"));
        assertEquals("2026-09-24T08:00:00Z", health.getDetails().get("nextRetryAt"));
    }

    @Test
    @DisplayName("DISABLED → UP（不做门禁）")
    void disabledIsUp() {
        KafkaTopicProvisioningStatus status = mock(KafkaTopicProvisioningStatus.class);
        when(status.state()).thenReturn(KafkaTopicProvisioningStatus.State.DISABLED);
        Health health = indicator(status).health();
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    @DisplayName("未启用 Provisioning（无状态 Bean）→ UP（不门禁）")
    void noStatusBeanIsUp() {
        KafkaTopicReadinessIndicator indicator = indicator(null);
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("disabled", health.getDetails().get("provisioning"));
    }
}
