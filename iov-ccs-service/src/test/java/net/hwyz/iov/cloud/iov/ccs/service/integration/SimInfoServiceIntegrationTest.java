package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.SimInfoService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SIM信息服务集成测试")
class SimInfoServiceIntegrationTest extends BaseTest {

    @Autowired
    private SimInfoService simInfoService;

    @Autowired
    private ManualSimService manualSimService;

    @Autowired
    private SimInfoRepository simInfoRepository;

    private SimInfo buildTestSim(String iccid) {
        return SimInfo.builder()
                .iccid(iccid)
                .imsi("460001234567890")
                .msisdn("8613800138000")
                .sourceMno("UNKNOWN")
                .sourceType("integration_test")
                .simStatus(1)
                .bindingStatus(0)
                .realnameStatus(1)
                .smsStatus(true)
                .dataStatus(true)
                .voiceStatus(true)
                .build();
    }

    @Test
    @DisplayName("手动保存SIM - 全链路验证")
    void manualSave_fullChain() {
        SimInfo simInfo = buildTestSim("99980000000000000001");

        manualSimService.saveSimInfo(simInfo);

        // 验证数据库中存在
        SimInfo saved = simInfoRepository.getByIccid("99980000000000000001");
        assertNotNull(saved);
        assertEquals(1, saved.getSimStatus());
        assertEquals(0, saved.getBindingStatus());
        assertEquals(1, saved.getRealnameStatus());
        assertTrue(saved.getSmsStatus());
        assertTrue(saved.getDataStatus());
        assertTrue(saved.getVoiceStatus());
        assertEquals("UNKNOWN", saved.getSourceMno());
        assertNotNull(saved.getSourceType());
    }

    @Test
    @DisplayName("手动保存SIM - ICCID重复抛异常")
    void manualSave_duplicateIccid() {
        SimInfo simInfo = buildTestSim("99980000000000000002");
        manualSimService.saveSimInfo(simInfo);

        assertThrows(ServiceException.class, () -> {
            manualSimService.saveSimInfo(buildTestSim("99980000000000000002"));
        });
    }

    @Test
    @DisplayName("批量保存SIM - 全链路验证")
    void batchSave_fullChain() {
        SimInfo sim1 = buildTestSim("99980000000000000003");
        SimInfo sim2 = buildTestSim("99980000000000000004");

        manualSimService.batchSaveSimInfo(List.of(sim1, sim2));

        assertTrue(simInfoRepository.existsByIccid("99980000000000000003"));
        assertTrue(simInfoRepository.existsByIccid("99980000000000000004"));
    }

    @Test
    @DisplayName("同步数据 - Hex解码+Upsert全链路")
    void syncData_fullChain() {
        String json = "[{\"iccid\":\"99980000000000000005\",\"imsi\":\"460001234567890\",\"msisdn\":\"13800138000\"}]";
        String hexData = HexFormat.of().formatHex(json.getBytes(StandardCharsets.UTF_8));

        manualSimService.syncData(hexData);

        SimInfo saved = simInfoRepository.getByIccid("99980000000000000005");
        assertNotNull(saved);
        assertEquals(1, saved.getSimStatus());
        assertEquals("UNKNOWN", saved.getSourceMno());
        assertEquals("sync_data", saved.getSourceType());
    }

    @Test
    @DisplayName("同步数据 - 重复ICCID执行Upsert更新")
    void syncData_upsert() {
        // 第一次插入
        SimInfo simInfo = buildTestSim("99980000000000000006");
        manualSimService.saveSimInfo(simInfo);

        // 第二次同步更新
        String json = "[{\"iccid\":\"99980000000000000006\",\"imsi\":\"460009999999999\",\"msisdn\":\"13900000000\"}]";
        String hexData = HexFormat.of().formatHex(json.getBytes(StandardCharsets.UTF_8));
        manualSimService.syncData(hexData);

        SimInfo updated = simInfoRepository.getByIccid("99980000000000000006");
        assertEquals("460009999999999", updated.getImsi());
    }

    @Test
    @DisplayName("保存SIM - 返回true/false验证")
    void saveSimInfo_returnsTrueOrFalse() {
        SimInfo simInfo = buildTestSim("99980000000000000007");

        assertTrue(simInfoService.saveSimInfo(simInfo));
        assertFalse(simInfoService.saveSimInfo(buildTestSim("99980000000000000007")));
    }

    // 注意：handleStorageEvent 使用 @Async("eventThreadPool")，需要配置线程池 Bean 才能测试
}
