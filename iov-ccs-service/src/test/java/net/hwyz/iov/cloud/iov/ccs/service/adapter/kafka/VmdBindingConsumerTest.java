package net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardBindingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

/**
 * VmdBindingConsumer 单元测试
 * <p>
 * 验证真实JSON解析（mock数据替换为真实字段）、TBOX过滤、非法JSON容错。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdBindingConsumer 测试")
class VmdBindingConsumerTest {

    private CardBindingService cardBindingService;
    private VmdBindingConsumer consumer;

    @BeforeEach
    void setUp() {
        cardBindingService = mock(CardBindingService.class);
        // 与Spring Boot默认ObjectMapper一致：注册JavaTimeModule以支持Instant解析
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // IOV-CCS-DSN-CR-004：显式业务 ZoneId（与 iov.web.jackson.zone-id 一致，禁止 systemDefault）
        consumer = new VmdBindingConsumer(cardBindingService, objectMapper, "Asia/Shanghai");
    }

    static final String TBOX_EVENT_JSON = """
            {"vin":"HWYZTEST900000001","bindingId":1,"partCode":"00000001AA","sn":"00000002AA00000001",
             "deviceCategory":"TBOX","vehicleNodeCode":"TBOX_5G",
             "iccid1":"89860400000000000001","iccid2":"89860600000000000001",
             "changeType":"BIND","occurredAt":"2026-09-23T03:39:54Z","seq":14000,
             "replay":true,"batchNum":"20260923113941","replayId":"9ca575d7-6372-4208-9bb7-ac7c92ec2639",
             "replayOperator":"1","replayedAt":"2026-10-10T06:56:49.791307223"}
            """;

    private ConsumerRecord<String, String> record(String key, String value) {
        return new ConsumerRecord<>("vmd.vehicle-part-binding.changed", 0, 0, key, value);
    }

    @Test
    @DisplayName("TBOX绑定事件：解析真实字段并交给绑定服务处理")
    void onMessage_tboxEvent_processes() {
        consumer.onMessage(record("HWYZTEST900000001", TBOX_EVENT_JSON));

        ArgumentCaptor<VmdBindingEvent> captor = ArgumentCaptor.forClass(VmdBindingEvent.class);
        verify(cardBindingService).handleBindingEvent(captor.capture());

        VmdBindingEvent event = captor.getValue();
        assertEquals("1", event.getBindingId());
        assertEquals("BIND", event.getChangeType());
        assertEquals("HWYZTEST900000001", event.getVin());
        assertEquals("TBOX", event.getDeviceCategory());
        assertEquals("00000002AA00000001", event.getTboxSn());
        assertEquals("89860400000000000001", event.getIccid1());
        assertEquals("89860600000000000001", event.getIccid2());
        assertEquals(14000L, event.getSeq());
        assertEquals(LocalDateTime.ofInstant(Instant.parse("2026-09-23T03:39:54Z"), ZoneId.of("Asia/Shanghai")),
                event.getOccurredAt());
    }

    @Test
    @DisplayName("非TBOX事件（DCU/CCU）：跳过不处理")
    void onMessage_nonTboxEvent_skips() {
        String dcuJson = TBOX_EVENT_JSON.replace("\"deviceCategory\":\"TBOX\"", "\"deviceCategory\":\"DCU\"");

        consumer.onMessage(record("HWYZTEST900000001", dcuJson));

        verify(cardBindingService, never()).handleBindingEvent(any());
    }

    @Test
    @DisplayName("非法JSON：记录错误日志，不抛出异常")
    void onMessage_invalidJson_logsError() {
        assertDoesNotThrow(() -> consumer.onMessage(record("HWYZTEST900000001", "{not-json")));
        verify(cardBindingService, never()).handleBindingEvent(any());
    }

    @Test
    @DisplayName("IOV-CCS-DSN-CR-004：occurredAt 转换不依赖 JVM 默认时区（UTC JVM 下仍按业务 ZoneId 解释）")
    void onMessage_tboxEvent_occurredAt_usesExplicitBusinessZone() {
        // 将 JVM 默认时区临时切换为 UTC，验证解析仍按显式业务 ZoneId（Asia/Shanghai）而非 systemDefault
        String originalTimezone = System.getProperty("user.timezone");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
        try {
            consumer.onMessage(record("HWYZTEST900000001", TBOX_EVENT_JSON));

            ArgumentCaptor<VmdBindingEvent> captor = ArgumentCaptor.forClass(VmdBindingEvent.class);
            verify(cardBindingService).handleBindingEvent(captor.capture());

            VmdBindingEvent event = captor.getValue();
            Instant occurredAtInstant = Instant.parse("2026-09-23T03:39:54Z");
            assertEquals(LocalDateTime.ofInstant(occurredAtInstant, ZoneId.of("Asia/Shanghai")),
                    event.getOccurredAt(), "occurredAt 必须按业务 ZoneId(Asia/Shanghai) 解释，而不是 JVM 默认时区");
        } finally {
            if (originalTimezone == null) {
                System.clearProperty("user.timezone");
            } else {
                System.setProperty("user.timezone", originalTimezone);
            }
            TimeZone.setDefault(TimeZone.getDefault());
        }
    }

    @Test
    @DisplayName("无ICCID事件：正常解析并交给绑定服务（内部跳过ICCID处理）")
    void onMessage_noIccid_stillParses() {
        String noIccidJson = TBOX_EVENT_JSON.replace("\"iccid1\":\"89860400000000000001\",\"iccid2\":\"89860600000000000001\",", "");

        consumer.onMessage(record("HWYZTEST900000001", noIccidJson));

        ArgumentCaptor<VmdBindingEvent> captor = ArgumentCaptor.forClass(VmdBindingEvent.class);
        verify(cardBindingService).handleBindingEvent(captor.capture());
        assertNotNull(captor.getValue());
        assertNull(captor.getValue().getIccid1());
        assertNull(captor.getValue().getIccid2());
    }
}
