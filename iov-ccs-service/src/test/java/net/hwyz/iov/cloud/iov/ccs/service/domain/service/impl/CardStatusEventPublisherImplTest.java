package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.CardBindingStatusEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStatusChangeEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.OutboxEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.OutboxEventRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics.CcsMetricsService;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 卡状态事件发布器单元测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 验证两类 Publisher 仅向目录标准 Topic 写入 outbox（Key/payload 语义不变），
 * 以及 Topic 未就绪时发布通道不启动（事件保持 PENDING）。
 *
 * @author hwyz_leo
 */
@DisplayName("CardStatusEventPublisherImpl 测试")
class CardStatusEventPublisherImplTest {

    private OutboxEventRepository outboxEventRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private ApplicationEventPublisher eventPublisher;
    private CcsMetricsService metricsService;
    private CcsKafkaTopicProperties topicProperties;
    private ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatus;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        eventPublisher = mock(ApplicationEventPublisher.class);
        metricsService = mock(CcsMetricsService.class);
        topicProperties = new CcsKafkaTopicProperties();
        provisioningStatus = mock(ObjectProvider.class);
        when(provisioningStatus.getIfAvailable()).thenReturn(null);
    }

    private CardStatusEventPublisherImpl publisher() {
        return new CardStatusEventPublisherImpl(outboxEventRepository, kafkaTemplate, objectMapper,
                eventPublisher, metricsService, topicProperties, provisioningStatus);
    }

    @Test
    @DisplayName("车卡绑定状态事件写入标准 Topic")
    void publishBindingStatus_usesStandardTopic() {
        CardBindingStatusEvent event = CardBindingStatusEvent.builder()
                .vin("VIN123")
                .iccid("ICCID001")
                .cardSlot(1)
                .bindingStatus(1)
                .sourceSeq(1L)
                .build();

        publisher().publishBindingStatus(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals("ccs.vehicle-sim-binding-status.changed", saved.getTopic());
        assertEquals("VIN123", saved.getMessageKey());
        assertEquals("CARD_BINDING_STATUS", saved.getEventType());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    @DisplayName("SIM 状态变更事件写入标准 Topic")
    void publishSimStatus_usesStandardTopic() {
        SimStatusChangeEvent event = SimStatusChangeEvent.builder()
                .vin("VIN123")
                .iccid("ICCID001")
                .simStatus(2)
                .realnameStatus(1)
                .build();

        publisher().publishSimStatus(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals("ccs.sim-status.changed", saved.getTopic());
        assertEquals("VIN123", saved.getMessageKey());
        assertEquals("SIM_STATUS", saved.getEventType());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    @DisplayName("Topic 未就绪时发布通道不启动（不查库、不发送）")
    void publishPendingEvents_skippedWhenNotReady() {
        KafkaTopicProvisioningStatus status = mock(KafkaTopicProvisioningStatus.class);
        when(status.state()).thenReturn(KafkaTopicProvisioningStatus.State.NOT_READY);
        when(status.missingTopics()).thenReturn(Set.of("ccs.sim-status.changed"));
        when(provisioningStatus.getIfAvailable()).thenReturn(status);

        int published = publisher().publishPendingEvents();

        assertEquals(0, published);
        verify(outboxEventRepository, never()).listPending(org.mockito.ArgumentMatchers.anyInt());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Topic 未就绪时失败重试通道不启动")
    void retryFailedEvents_skippedWhenNotReady() {
        KafkaTopicProvisioningStatus status = mock(KafkaTopicProvisioningStatus.class);
        when(status.state()).thenReturn(KafkaTopicProvisioningStatus.State.NOT_READY);
        when(provisioningStatus.getIfAvailable()).thenReturn(status);

        int retried = publisher().retryFailedEvents();

        assertEquals(0, retried);
        verify(outboxEventRepository, never()).listRetryable(org.mockito.ArgumentMatchers.anyInt());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Topic 就绪时按 outbox 存储的标准 Topic 发送，Key/payload 不变")
    void publishPendingEvents_readySendsToStoredStandardTopic() throws Exception {
        when(provisioningStatus.getIfAvailable()).thenReturn(null);

        OutboxEvent pending = OutboxEvent.builder()
                .id(1L)
                .eventType("SIM_STATUS")
                .aggregateId("ICCID001")
                .payload("{\"vin\":\"VIN123\",\"iccid\":\"ICCID001\"}")
                .status("PENDING")
                .topic("ccs.sim-status.changed")
                .messageKey("VIN123")
                .retryCount(0)
                .maxRetry(3)
                .build();
        when(outboxEventRepository.listPending(100)).thenReturn(List.of(pending));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("ccs.sim-status.changed"), eq("VIN123"), anyString())).thenReturn(future);

        int published = publisher().publishPendingEvents();

        assertEquals(1, published);
        verify(kafkaTemplate).send("ccs.sim-status.changed", "VIN123", "{\"vin\":\"VIN123\",\"iccid\":\"ICCID001\"}");
        verify(outboxEventRepository).update(pending);
        assertNull(pending.getFailureReason());
    }
}
