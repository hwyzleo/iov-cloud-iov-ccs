package net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardBindingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * VMD绑定事件消费者
 * <p>
 * 订阅VMD VehiclePartBindingChangedEvent，按deviceCategory=TBOX过滤
 * 消费组独立，Key=VIN保证同车有序
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdBindingConsumer {

    private static final String DEVICE_CATEGORY_TBOX = "TBOX";

    private final CardBindingService cardBindingService;
    private final ObjectMapper objectMapper;
    /**
     * 业务墙上时间 ZoneId（复用 iov.web.jackson.zone-id，禁止 systemDefault 猜测业务语义）
     */
    private final ZoneId businessZoneId;

    public VmdBindingConsumer(CardBindingService cardBindingService,
                              ObjectMapper objectMapper,
                              @Value("${iov.web.jackson.zone-id}") String businessZoneId) {
        this.cardBindingService = cardBindingService;
        this.objectMapper = objectMapper;
        this.businessZoneId = ZoneId.of(businessZoneId);
    }

    /**
     * 消费VMD绑定事件
     * <p>
     * 采用默认 BATCH 自动提交（与平台其它服务一致），依赖业务侧幂等消费
     * （bindingId+changeType 幂等、按 seq 处理乱序），不需要手动确认。
     *
     * @param record Kafka消息
     */
    @KafkaListener(
            topics = "${kafka.topic.vmd-binding:vmd.vehicle-part-binding.changed}",
            groupId = "${kafka.group-id.ccs-vmd-binding:iov-ccs-vmd-binding}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("收到VMD绑定事件: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        try {
            // 解析事件
            VmdBindingEvent event = parseEvent(record.value());

            // 按deviceCategory=TBOX过滤
            if (!isTboxEvent(event)) {
                log.debug("非TBOX事件，跳过: bindingId={}, deviceCategory={}",
                        event.getBindingId(), event.getDeviceCategory());
                return;
            }

            // 处理绑定事件
            cardBindingService.handleBindingEvent(event);

            log.info("VMD绑定事件处理完成: bindingId={}, vin={}", event.getBindingId(), event.getVin());

        } catch (Exception e) {
            log.error("VMD绑定事件处理失败: offset={}", record.offset(), e);
            // TODO: 实现重试逻辑和死信队列
            // 失败不抛出，依赖自动提交继续消费；重试/DLQ 落地后再决定是否改为失败不提交
        }
    }

    /**
     * 解析事件JSON并映射为领域事件
     */
    private VmdBindingEvent parseEvent(String json) {
        try {
            VmdPartBindingChangedMessage message = objectMapper.readValue(json, VmdPartBindingChangedMessage.class);
            return VmdBindingEvent.builder()
                    .bindingId(message.getBindingId() != null ? String.valueOf(message.getBindingId()) : null)
                    .changeType(message.getChangeType())
                    .vin(message.getVin())
                    .tboxSn(message.getSn())
                    .deviceCategory(message.getDeviceCategory())
                    .iccid1(message.getIccid1())
                    .iccid2(message.getIccid2())
                    .seq(message.getSeq())
                    .occurredAt(message.getOccurredAt() != null
                            ? LocalDateTime.ofInstant(message.getOccurredAt(), businessZoneId)
                            : null)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("VMD绑定事件JSON解析失败: " + json, e);
        }
    }

    /**
     * 判断是否为TBOX事件
     */
    private boolean isTboxEvent(VmdBindingEvent event) {
        return DEVICE_CATEGORY_TBOX.equals(event.getDeviceCategory());
    }
}
