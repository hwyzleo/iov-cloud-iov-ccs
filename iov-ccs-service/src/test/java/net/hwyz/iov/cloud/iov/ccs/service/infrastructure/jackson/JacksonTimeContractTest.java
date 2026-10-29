package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.framework.web.jackson.IovEpochMillisWriteModule;
import net.hwyz.iov.cloud.framework.web.jackson.IovJavaTimeReadModule;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka.VmdPartBindingChangedMessage;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open.OpenCuccCallbackController;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.CardBindingStatusEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStatusChangeEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IOV-CCS-DSN-CR-004 JSON java.time 时间契约单元测试
 * <p>
 * 使用框架 FW-WEB-DSN-CR-001 P1 的真实 codec 模块（IovJavaTimeReadModule / IovEpochMillisWriteModule）
 * 按生产装配顺序（JavaTimeModule → 框架读模块 → 框架写模块）构建受管 ObjectMapper，
 * 验证组织自有 JSON java.time 契约（date-format=epoch-millis, zone-id=Asia/Shanghai）：
 * - 写单一：SimInfo / 状态事件的 java.time 字段输出 13 位 epoch-millis（保持旧输出，不切 ISO）
 * - 读兼容：ISO 字符串 / 10 位秒 / 13 位毫秒 双读归一到同一时刻
 * - 非法格式 fail-closed 拒绝（数组 / 秒小数 / 溢出 / 非法 ISO / 空串 / 错误类型 / 窗口外 0 / 负值）
 * - null 保持字段可空性
 * - 排除范围：CUCC timestamp(long) 整数毫秒、CMCC ts(String)、普通 long 不受 java.time 策略影响
 * <p>
 * 真实 Boot 装配的端到端验证见集成测试（需 CI/环境执行）。
 *
 * @author hwyz_leo
 */
@DisplayName("IOV-CCS-DSN-CR-004 JSON java.time 时间契约测试")
class JacksonTimeContractTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        // 与 Boot 默认装配一致：JavaTimeModule 后，框架 postConfigurer 追加读/写模块（后注册覆盖同类型 serializer）
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new IovJavaTimeReadModule(BUSINESS_ZONE));
        mapper.registerModule(new IovEpochMillisWriteModule(BUSINESS_ZONE));
    }

    // ========== 写单一：epoch-millis 输出 golden（FR-004-2：保持旧毫秒输出） ==========

    @Test
    @DisplayName("SimInfo.createdTime/updatedTime 输出 13 位 epoch 毫秒（MVC /api/mpt/simInfo/v1 列表与详情）")
    void simInfo_write_epochMillis() throws Exception {
        LocalDateTime created = LocalDateTime.of(2026, 10, 14, 14, 26, 13);
        LocalDateTime updated = LocalDateTime.of(2026, 10, 14, 15, 30, 0);
        SimInfo simInfo = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("8613800138000")
                .sourceMno("CUCC")
                .sourceType("cucc_push")
                .simStatus(1)
                .createdTime(created)
                .updatedTime(updated)
                .build();

        String json = mapper.writeValueAsString(simInfo);

        long createdMillis = created.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long updatedMillis = updated.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        assertTrue(json.contains("\"createdTime\":" + createdMillis), "createdTime 应为 epoch 毫秒，实际: " + json);
        assertTrue(json.contains("\"updatedTime\":" + updatedMillis), "updatedTime 应为 epoch 毫秒，实际: " + json);
        // 不得输出 ISO 字符串或数组
        assertTrue(!json.contains("2026-10-14"), "不得输出 ISO 字符串，实际: " + json);
    }

    @Test
    @DisplayName("SimStatusChangeEvent.occurredAt 输出 13 位 epoch 毫秒（Kafka ccs.sim-status.changed）")
    void simStatusEvent_write_epochMillis() throws Exception {
        LocalDateTime occurred = LocalDateTime.of(2026, 10, 14, 14, 26, 13);
        SimStatusChangeEvent event = SimStatusChangeEvent.builder()
                .version(1)
                .vin("HWYZTEST900000001")
                .iccid("89860123456789012345")
                .simStatus(1)
                .realnameStatus(1)
                .occurredAt(occurred)
                .build();

        String json = mapper.writeValueAsString(event);

        long occurredMillis = occurred.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        assertTrue(json.contains("\"occurredAt\":" + occurredMillis), "occurredAt 应为 epoch 毫秒，实际: " + json);
        assertTrue(!json.contains("2026-10-14"), "不得输出 ISO 字符串，实际: " + json);
    }

    @Test
    @DisplayName("CardBindingStatusEvent.occurredAt 输出 13 位 epoch 毫秒（Kafka ccs.vehicle-sim-binding-status.changed）")
    void cardBindingStatusEvent_write_epochMillis() throws Exception {
        LocalDateTime occurred = LocalDateTime.of(2026, 10, 14, 14, 26, 13);
        CardBindingStatusEvent event = CardBindingStatusEvent.builder()
                .version(1)
                .vin("HWYZTEST900000001")
                .iccid("89860123456789012345")
                .cardSlot(1)
                .bindingStatus(1)
                .sourceSeq(14000L)
                .occurredAt(occurred)
                .build();

        String json = mapper.writeValueAsString(event);

        long occurredMillis = occurred.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        assertTrue(json.contains("\"occurredAt\":" + occurredMillis), "occurredAt 应为 epoch 毫秒，实际: " + json);
        assertTrue(!json.contains("2026-10-14"), "不得输出 ISO 字符串，实际: " + json);
    }

    // ========== 读兼容：双读（FR-004-2：读端与输出模式解耦，始终兼容已批准 ISO + 旧数字） ==========

    @Test
    @DisplayName("Instant 双读：ISO / 10位秒 / 13位毫秒 归一到同一时刻（VMD 入站 occurredAt）")
    void instant_doubleRead_sameInstant() throws Exception {
        Instant instant = Instant.parse("2026-10-14T06:26:13Z");
        long millis = instant.toEpochMilli();
        long seconds = instant.getEpochSecond();

        Instant fromIso = mapper.readValue("{\"occurredAt\":\"2026-10-14T06:26:13Z\"}", VmdPartBindingChangedMessage.class).getOccurredAt();
        Instant fromMillis = mapper.readValue("{\"occurredAt\":" + millis + "}", VmdPartBindingChangedMessage.class).getOccurredAt();
        Instant fromSeconds = mapper.readValue("{\"occurredAt\":" + seconds + "}", VmdPartBindingChangedMessage.class).getOccurredAt();

        assertEquals(instant, fromIso, "ISO 输入应归一");
        assertEquals(instant, fromMillis, "13 位毫秒输入应归一");
        assertEquals(instant, fromSeconds, "10 位秒输入应归一");
    }

    @Test
    @DisplayName("LocalDateTime 双读：ISO 墙上时间 / 13 位毫秒（按业务 ZoneId）归一到同一时刻")
    void localDateTime_doubleRead_sameLocal() throws Exception {
        LocalDateTime expected = LocalDateTime.of(2026, 10, 14, 14, 26, 13);
        long millis = expected.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();

        LocalDateTime fromIso = mapper.readValue("{\"occurredAt\":\"2026-10-14T14:26:13\"}", SimStatusChangeEvent.class).getOccurredAt();
        LocalDateTime fromMillis = mapper.readValue("{\"occurredAt\":" + millis + "}", SimStatusChangeEvent.class).getOccurredAt();

        assertEquals(expected, fromIso, "ISO 墙上时间输入应保持");
        assertEquals(expected, fromMillis, "13 位毫秒输入应按业务 ZoneId 归一");
    }

    // ========== 非法格式 fail-closed 拒绝（RD-001-3 / CR §14.2） ==========

    @Test
    @DisplayName("拒绝：数组 / 秒小数 / 溢出 / 非法 ISO / 空串 / 错误类型 / 窗口外 0 / 负值")
    void illegalFormats_failClosed() {
        String[] illegalValues = {
                "[\"2026-10-14T06:26:13Z\",\"2026-10-14T07:00:00Z\"]", // 数组
                "1768371973.123",                                      // 秒小数
                "99999999999999999",                                   // 溢出
                "not-a-date",                                          // 非法 ISO
                "\"\"",                                                // 空串
                "true",                                                // 错误类型
                "0",                                                   // 窗口外
                "-1"                                                   // 负值
        };
        for (String value : illegalValues) {
            String json = "{\"occurredAt\":" + value + "}";
            assertThrows(JsonProcessingException.class,
                    () -> mapper.readValue(json, VmdPartBindingChangedMessage.class),
                    "Instant 字段应拒绝: " + json);
            assertThrows(JsonProcessingException.class,
                    () -> mapper.readValue(json, SimStatusChangeEvent.class),
                    "LocalDateTime 字段应拒绝: " + json);
        }
    }

    // ========== null 可空性 ==========

    @Test
    @DisplayName("null 保持字段可空性，不转换为 epoch 0")
    void null_staysNull() throws Exception {
        SimStatusChangeEvent event = mapper.readValue("{\"occurredAt\":null}", SimStatusChangeEvent.class);
        assertNull(event.getOccurredAt(), "occurredAt=null 必须保持 null");

        String json = mapper.writeValueAsString(SimInfo.builder().createdTime(null).build());
        assertTrue(json.contains("\"createdTime\":null"), "createdTime=null 输出必须为 null，实际: " + json);
    }

    // ========== 排除范围（FR-004-4 / CR §14.3，保持不变） ==========

    @Test
    @DisplayName("排除：CUCC timestamp(long) 整数毫秒不受 java.time 策略影响（签名/防重放输入原样）")
    void cucc_timestamp_long_untouched() throws Exception {
        long timestamp = 1718210000000L;
        String json = "{\"appid\":\"xxx\",\"timestamp\":" + timestamp + ",\"nonce\":\"a1b2c3\",\"signature\":\"sig\",\"batchNo\":\"B1\",\"data\":[]}";

        OpenCuccCallbackController.CuccSimInfoRequest request =
                mapper.readValue(json, OpenCuccCallbackController.CuccSimInfoRequest.class);

        assertEquals(timestamp, request.getTimestamp(), "timestamp 必须保持整数毫秒数值");
        // 序列化回 JSON 仍是数字，而非 ISO 字符串
        String out = mapper.writeValueAsString(request);
        assertTrue(out.contains("\"timestamp\":" + timestamp), "序列化后 timestamp 仍为数字，实际: " + out);
    }

    @Test
    @DisplayName("排除：CMCC ts(String) 与普通 long 字段不受 java.time 策略影响")
    void cmcc_ts_string_untouched() throws Exception {
        String json = "{\"vin\":\"HWYZTEST900000001\",\"iccid\":\"89860123456789012345\",\"cardSlot\":1,\"bindingStatus\":1,\"sourceSeq\":14000}";

        CardBindingStatusEvent event = mapper.readValue(json, CardBindingStatusEvent.class);

        assertEquals(14000L, event.getSourceSeq(), "普通 long sourceSeq 必须保持数值");
        assertEquals("HWYZTEST900000001", event.getVin(), "字符串字段原样");
    }

    @Test
    @DisplayName("排除：CMCC ts String 原样读写（SHA256(eSecret+timestamp) 密钥派生输入）")
    void cmcc_ts_string_readWrite() throws Exception {
        // CmccFileRequestRecord.ts 为 String；JSON 往返必须保持字符串原样，不被解释为时间
        String json = "{\"fileId\":\"F1\",\"ts\":\"1718200000000\",\"status\":\"APPLYING\"}";

        var record = mapper.readValue(json,
                net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord.class);

        assertEquals("1718200000000", record.getTs(), "ts 字符串必须原样保持");
        String out = mapper.writeValueAsString(record);
        assertTrue(out.contains("\"ts\":\"1718200000000\""), "序列化后 ts 仍为字符串，实际: " + out);
    }
}
