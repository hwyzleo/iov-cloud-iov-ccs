package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("手动录入SIM服务集成测试")
class ManualSimServiceIntegrationTest extends BaseTest {

    @Autowired
    private ManualSimService manualSimService;

    @Autowired
    private SimInfoRepository simInfoRepository;

    private SimInfo buildTestSim(String iccid, String imsi, String msisdn) {
        return SimInfo.builder()
                .iccid(iccid)
                .imsi(imsi)
                .msisdn(msisdn)
                .build();
    }

    @Test
    @DisplayName("MSISDN规范化 - 11位自动加86前缀")
    void saveSimInfo_msisdnNormalization() {
        SimInfo simInfo = buildTestSim("99970000000000000001", "460001234567890", "13800138000");

        manualSimService.saveSimInfo(simInfo);

        SimInfo saved = simInfoRepository.getByIccid("99970000000000000001");
        assertEquals("8613800138000", saved.getMsisdn());
    }

    @Test
    @DisplayName("MSISDN规范化 - 已有86前缀保持不变")
    void saveSimInfo_msisdnAlreadyNormalized() {
        SimInfo simInfo = buildTestSim("99970000000000000002", "460001234567890", "8613800138000");

        manualSimService.saveSimInfo(simInfo);

        SimInfo saved = simInfoRepository.getByIccid("99970000000000000002");
        assertEquals("8613800138000", saved.getMsisdn());
    }

    @Test
    @DisplayName("ICCID格式校验 - 非法格式抛异常")
    void saveSimInfo_invalidIccid() {
        SimInfo simInfo = buildTestSim("invalid", "460001234567890", "13800138000");

        assertThrows(IllegalArgumentException.class, () -> manualSimService.saveSimInfo(simInfo));
    }

    @Test
    @DisplayName("IMSI格式校验 - 非法格式抛异常")
    void saveSimInfo_invalidImsi() {
        SimInfo simInfo = buildTestSim("99970000000000000003", "invalid", "13800138000");

        assertThrows(IllegalArgumentException.class, () -> manualSimService.saveSimInfo(simInfo));
    }

    @Test
    @DisplayName("批量保存 - 部分失败抛BatchSaveException")
    void batchSaveSimInfo_partialFail() {
        SimInfo sim1 = buildTestSim("99970000000000000004", "460001234567890", "13800138000");
        SimInfo sim2 = buildTestSim("99970000000000000005", "460001234567891", "13800138001");

        manualSimService.batchSaveSimInfo(List.of(sim1, sim2));

        // 再次批量保存相同ICCID
        SimInfo dup1 = buildTestSim("99970000000000000004", "460001234567890", "13800138000");
        SimInfo sim3 = buildTestSim("99970000000000000006", "460001234567892", "13800138002");

        BatchSaveException ex = assertThrows(BatchSaveException.class,
                () -> manualSimService.batchSaveSimInfo(List.of(dup1, sim3)));

        assertTrue(ex.getFailedIccids().contains("99970000000000000004"));
        // sim3 should still be saved
        assertTrue(simInfoRepository.existsByIccid("99970000000000000006"));
    }

    @Test
    @DisplayName("同步数据 - 空Hex数据抛异常")
    void syncData_emptyHexData() {
        assertThrows(ServiceException.class, () -> manualSimService.syncData(""));
        assertThrows(ServiceException.class, () -> manualSimService.syncData(null));
    }

    @Test
    @DisplayName("同步数据 - 无效Hex数据抛异常")
    void syncData_invalidHexData() {
        assertThrows(Exception.class, () -> manualSimService.syncData("not_hex"));
    }

    @Test
    @DisplayName("同步数据 - 多条记录批量Upsert")
    void syncData_multiRecords() {
        String json = "["
                + "{\"iccid\":\"99970000000000000007\",\"imsi\":\"460001234567890\",\"msisdn\":\"13800138000\"},"
                + "{\"iccid\":\"99970000000000000008\",\"imsi\":\"460001234567891\",\"msisdn\":\"13800138001\"},"
                + "{\"iccid\":\"99970000000000000009\",\"imsi\":\"460001234567892\",\"msisdn\":\"13800138002\"}"
                + "]";
        String hexData = HexFormat.of().formatHex(json.getBytes(StandardCharsets.UTF_8));

        manualSimService.syncData(hexData);

        assertTrue(simInfoRepository.existsByIccid("99970000000000000007"));
        assertTrue(simInfoRepository.existsByIccid("99970000000000000008"));
        assertTrue(simInfoRepository.existsByIccid("99970000000000000009"));
    }
}
