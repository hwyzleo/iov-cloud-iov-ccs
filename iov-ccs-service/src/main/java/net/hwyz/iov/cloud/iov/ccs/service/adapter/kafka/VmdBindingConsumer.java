package net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardBindingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * VMD绑定事件消费者
 * <p>
 * 订阅VMD VehiclePartBindingChangedEvent，按part_type=TBOX过滤
 * 消费组独立，Key=VIN保证同车有序
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmdBindingConsumer {

    private static final String PART_TYPE_TBOX = "TBOX";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CardBindingService cardBindingService;

    /**
     * 消费VMD绑定事件
     *
     * @param record        Kafka消息
     * @param acknowledgment 确认
     */
    @KafkaListener(
            topics = "${kafka.topic.vmd-binding:vehicle-part-binding-changed}",
            groupId = "${kafka.group-id.ccs-vmd-binding:iov-ccs-vmd-binding}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("收到VMD绑定事件: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        try {
            // 解析事件
            VmdBindingEvent event = parseEvent(record.value());

            // 按part_type=TBOX过滤
            if (!isTboxEvent(event)) {
                log.debug("非TBOX事件，跳过: bindingId={}", event.getBindingId());
                acknowledgment.acknowledge();
                return;
            }

            // 处理绑定事件
            cardBindingService.handleBindingEvent(event);

            // 确认消费
            acknowledgment.acknowledge();
            log.info("VMD绑定事件处理完成: bindingId={}, vin={}", event.getBindingId(), event.getVin());

        } catch (Exception e) {
            log.error("VMD绑定事件处理失败: offset={}", record.offset(), e);
            // TODO: 实现重试逻辑和死信队列
            // 消费失败进退避重试，超限入DLQ + 告警
            acknowledgment.acknowledge();
        }
    }

    /**
     * 解析事件JSON
     */
    private VmdBindingEvent parseEvent(String json) {
        // TODO: 实现真实的JSON解析
        // Mock实现
        return VmdBindingEvent.builder()
                .bindingId("mock-binding-id")
                .changeType("BIND")
                .vin("mock-vin")
                .tboxSn("mock-tbox-sn")
                .iccid1("mock-iccid1")
                .iccid2(null)
                .seq(1L)
                .occurredAt(LocalDateTime.now())
                .originalEventId("mock-event-id")
                .build();
    }

    /**
     * 判断是否为TBOX事件
     */
    private boolean isTboxEvent(VmdBindingEvent event) {
        // TODO: 实现真实的part_type判断
        // Mock实现：默认为TBOX事件
        return true;
    }
}
