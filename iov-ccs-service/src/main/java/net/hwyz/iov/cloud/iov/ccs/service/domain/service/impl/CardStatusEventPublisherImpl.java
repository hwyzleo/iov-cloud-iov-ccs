package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.CardBindingStatusEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStatusChangeEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.OutboxEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.OutboxEventRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardStatusEventPublisher;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics.CcsMetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡状态事件发布器实现
 * <p>
 * 采用 Outbox Pattern 保证至少一次投递
 * CCS 只发不收，不反向写任何下游域
 * 发布失败仅告警、不阻断绑定 / 入库主流程
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardStatusEventPublisherImpl implements CardStatusEventPublisher {

    private static final String EVENT_TYPE_CARD_BINDING = "CARD_BINDING_STATUS";
    private static final String EVENT_TYPE_SIM_STATUS = "SIM_STATUS";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int DEFAULT_MAX_RETRY = 3;
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CcsMetricsService metricsService;

    @Value("${kafka.topic.card-binding-status:card-binding-status-changed}")
    private String cardBindingStatusTopic;

    @Value("${kafka.topic.sim-status:sim-status-changed}")
    private String simStatusTopic;

    @Override
    @Transactional
    public void publishBindingStatus(CardBindingStatusEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(EVENT_TYPE_CARD_BINDING)
                    .aggregateId(event.getVin())
                    .payload(payload)
                    .status(STATUS_PENDING)
                    .topic(cardBindingStatusTopic)
                    .messageKey(event.getVin())
                    .retryCount(0)
                    .maxRetry(DEFAULT_MAX_RETRY)
                    .createdTime(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("车卡绑定状态变更事件已写入 outbox: vin={}, iccid={}, bindingStatus={}",
                    event.getVin(), event.getIccid(), event.getBindingStatus());

            metricsService.recordEventOutboxSaved();

        } catch (JsonProcessingException e) {
            log.error("序列化车卡绑定状态变更事件失败", e);
            metricsService.recordEventSerializeFail();
            // 发布失败仅告警、不阻断主流程
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.EVENT_SERIALIZE_FAIL)
                    .refKey(event.getVin())
                    .message("序列化车卡绑定状态变更事件失败")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    @Transactional
    public void publishSimStatus(SimStatusChangeEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(EVENT_TYPE_SIM_STATUS)
                    .aggregateId(event.getIccid())
                    .payload(payload)
                    .status(STATUS_PENDING)
                    .topic(simStatusTopic)
                    .messageKey(event.getVin())
                    .retryCount(0)
                    .maxRetry(DEFAULT_MAX_RETRY)
                    .createdTime(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("SIM状态变更事件已写入 outbox: iccid={}, simStatus={}, realnameStatus={}",
                    event.getIccid(), event.getSimStatus(), event.getRealnameStatus());

            metricsService.recordEventOutboxSaved();

        } catch (JsonProcessingException e) {
            log.error("序列化SIM状态变更事件失败", e);
            metricsService.recordEventSerializeFail();
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.EVENT_SERIALIZE_FAIL)
                    .refKey(event.getIccid())
                    .message("序列化SIM状态变更事件失败")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public int publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.listPending(BATCH_SIZE);
        int successCount = 0;

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                kafkaTemplate.send(
                        outboxEvent.getTopic(),
                        outboxEvent.getMessageKey(),
                        outboxEvent.getPayload()
                ).get();

                outboxEvent.setStatus(STATUS_PUBLISHED);
                outboxEvent.setPublishedTime(LocalDateTime.now());
                outboxEventRepository.update(outboxEvent);

                successCount++;
                metricsService.recordEventPublished();

                log.debug("事件发布成功: id={}, type={}, topic={}",
                        outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getTopic());

            } catch (Exception e) {
                handlePublishFailure(outboxEvent, e);
            }
        }

        return successCount;
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public int retryFailedEvents() {
        List<OutboxEvent> retryableEvents = outboxEventRepository.listRetryable(BATCH_SIZE);
        int successCount = 0;

        for (OutboxEvent outboxEvent : retryableEvents) {
            try {
                kafkaTemplate.send(
                        outboxEvent.getTopic(),
                        outboxEvent.getMessageKey(),
                        outboxEvent.getPayload()
                ).get();

                outboxEvent.setStatus(STATUS_PUBLISHED);
                outboxEvent.setPublishedTime(LocalDateTime.now());
                outboxEventRepository.update(outboxEvent);

                successCount++;
                metricsService.recordEventRetrySuccess();

                log.info("事件重试发布成功: id={}, type={}, retryCount={}",
                        outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getRetryCount());

            } catch (Exception e) {
                handlePublishFailure(outboxEvent, e);
            }
        }

        return successCount;
    }

    /**
     * 处理发布失败
     */
    private void handlePublishFailure(OutboxEvent outboxEvent, Exception e) {
        log.error("事件发布失败: id={}, type={}, retryCount={}",
                outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getRetryCount(), e);

        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
        outboxEvent.setFailureReason(e.getMessage());

        if (outboxEvent.getRetryCount() >= outboxEvent.getMaxRetry()) {
            outboxEvent.setStatus(STATUS_FAILED);
            metricsService.recordEventPublishFailed();

            // 发布失败连续超阈告警
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.EVENT_PUBLISH_FAIL)
                    .refKey(outboxEvent.getAggregateId())
                    .message("事件发布失败超过最大重试次数")
                    .detail("eventType=" + outboxEvent.getEventType() +
                            ", retryCount=" + outboxEvent.getRetryCount() +
                            ", error=" + e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }

        outboxEventRepository.update(outboxEvent);
        metricsService.recordEventPublishFail();
    }
}
