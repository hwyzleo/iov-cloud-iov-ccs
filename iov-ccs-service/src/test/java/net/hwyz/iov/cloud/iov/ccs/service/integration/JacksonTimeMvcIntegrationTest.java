package net.hwyz.iov.cloud.iov.ccs.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.framework.security.auth.AuthLogic;
import net.hwyz.iov.cloud.framework.security.auth.AuthUtil;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IOV-CCS-DSN-CR-004 JSON java.time 时间契约 MVC 集成测试
 * <p>
 * 在真实 Boot 装配（含 framework-web-starter IovJacksonAutoConfiguration 与启动探针）
 * 下验证组织自有 JSON java.time 输出契约（date-format=epoch-millis, zone-id=Asia/Shanghai）：
 * - GET /api/mpt/simInfo/v1/{iccid} 与 /list 的 createdTime/updatedTime 输出 13 位 epoch 毫秒
 * - 不得输出 ISO 字符串（本阶段保持旧毫秒输出，不切 ISO）
 * - 生产 ObjectMapper 双读（ISO / 10 位秒 / 13 位毫秒 → 同一时刻）
 * - 排除范围：CUCC timestamp(long) 整数毫秒不受 java.time 策略影响
 * <p>
 * 使用 webEnvironment=MOCK + MockMvc（不占用真实端口，避免与其它集成测试上下文端口冲突），
 * MockMvc 仍走完整 MVC → HttpMessageConverter → 生产 ObjectMapper 序列化链路。
 * 依赖真实基础设施（dev profile），本地无环境时由 CI/部署环境执行（CR §14.4 证据门禁）。
 *
 * @author hwyz_leo
 */
@DisplayName("IOV-CCS-DSN-CR-004 MVC JSON 时间契约集成测试")
@Rollback
@Transactional
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JacksonTimeMvcIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ICCID = "89860123456789012345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManualSimService manualSimService;

    @BeforeEach
    void setup() {
        AuthUtil.authLogic = Mockito.mock(AuthLogic.class);
    }

    private SimInfo buildSim() {
        return SimInfo.builder()
                .id(1L)
                .iccid(ICCID)
                .imsi("460001234567890")
                .msisdn("8613800138000")
                .sourceMno("CUCC")
                .sourceType("cucc_push")
                .simStatus(1)
                .bindingStatus(0)
                .realnameStatus(1)
                .smsStatus(true)
                .dataStatus(true)
                .voiceStatus(true)
                .createdTime(LocalDateTime.of(2026, 10, 14, 14, 26, 13))
                .updatedTime(LocalDateTime.of(2026, 10, 14, 15, 30, 0))
                .build();
    }

    @Test
    @DisplayName("详情接口：createdTime/updatedTime 输出 13 位 epoch 毫秒（非 ISO 字符串）")
    void getInfo_jsonTimeFields_epochMillis() throws Exception {
        when(manualSimService.getSimInfo(ICCID)).thenReturn(buildSim());

        MvcResult result = mockMvc.perform(get("/api/mpt/simInfo/v1/{iccid}", ICCID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");

        long createdMillis = LocalDateTime.of(2026, 10, 14, 14, 26, 13)
                .atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long updatedMillis = LocalDateTime.of(2026, 10, 14, 15, 30, 0)
                .atZone(BUSINESS_ZONE).toInstant().toEpochMilli();

        assertTrue(data.has("createdTime"), "响应必须包含 createdTime");
        assertEquals(createdMillis, data.get("createdTime").asLong(), "createdTime 必须为 epoch 毫秒");
        assertEquals(updatedMillis, data.get("updatedTime").asLong(), "updatedTime 必须为 epoch 毫秒");
        assertFalse(data.get("createdTime").isTextual(), "不得输出 ISO 字符串");
        assertFalse(result.getResponse().getContentAsString().contains("2026-10-14"), "响应不得包含 ISO 时间字符串");
    }

    @Test
    @DisplayName("列表接口：分页结果内时间字段输出 epoch 毫秒")
    void list_jsonTimeFields_epochMillis() throws Exception {
        when(manualSimService.listSimInfo(any())).thenReturn(java.util.List.of(buildSim()));

        MvcResult result = mockMvc.perform(get("/api/mpt/simInfo/v1/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("list");
        assertTrue(list.isArray() && list.size() == 1, "列表应返回 1 条数据");

        JsonNode item = list.get(0);
        long createdMillis = LocalDateTime.of(2026, 10, 14, 14, 26, 13)
                .atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        assertEquals(createdMillis, item.get("createdTime").asLong(), "createdTime 必须为 epoch 毫秒");
        assertFalse(item.get("createdTime").isTextual(), "不得输出 ISO 字符串");
        assertFalse(result.getResponse().getContentAsString().contains("2026-10-14"), "响应不得包含 ISO 时间字符串");
    }

    @Test
    @DisplayName("生产 ObjectMapper 双读：ISO / 13 位毫秒 / 10 位秒 输入归一到同一时刻（VMD 入站 Instant）")
    void productionMapper_doubleRead_sameInstant() throws Exception {
        Instant expected = Instant.parse("2026-10-14T06:26:13Z");
        long millis = expected.toEpochMilli();
        long seconds = expected.getEpochSecond();

        String iso = "{\"vin\":\"V1\",\"deviceCategory\":\"TBOX\",\"occurredAt\":\"2026-10-14T06:26:13Z\"}";
        String millisJson = "{\"vin\":\"V1\",\"deviceCategory\":\"TBOX\",\"occurredAt\":" + millis + "}";
        String secondsJson = "{\"vin\":\"V1\",\"deviceCategory\":\"TBOX\",\"occurredAt\":" + seconds + "}";

        var fromIso = objectMapper.readValue(iso,
                net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka.VmdPartBindingChangedMessage.class);
        var fromMillis = objectMapper.readValue(millisJson,
                net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka.VmdPartBindingChangedMessage.class);
        var fromSeconds = objectMapper.readValue(secondsJson,
                net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka.VmdPartBindingChangedMessage.class);

        assertEquals(expected, fromIso.getOccurredAt());
        assertEquals(expected, fromMillis.getOccurredAt());
        assertEquals(expected, fromSeconds.getOccurredAt());
    }

    @Test
    @DisplayName("生产 ObjectMapper 排除：CUCC timestamp(long) 保持整数毫秒")
    void productionMapper_cuccTimestamp_untouched() throws Exception {
        long timestamp = 1718210000000L;
        String json = "{\"appid\":\"xxx\",\"timestamp\":" + timestamp + ",\"nonce\":\"a1b2c3\",\"signature\":\"sig\",\"batchNo\":\"B1\",\"data\":[]}";

        var request = objectMapper.readValue(json,
                net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open.OpenCuccCallbackController.CuccSimInfoRequest.class);

        assertEquals(timestamp, request.getTimestamp());
        String out = objectMapper.writeValueAsString(request);
        assertTrue(out.contains("\"timestamp\":" + timestamp), "CUCC timestamp 必须保持整数毫秒，实际: " + out);
    }
}
