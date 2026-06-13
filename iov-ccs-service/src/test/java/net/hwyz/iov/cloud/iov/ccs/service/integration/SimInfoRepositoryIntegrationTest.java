package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SIM信息仓储集成测试")
class SimInfoRepositoryIntegrationTest extends BaseTest {

    @Autowired
    private SimInfoRepository simInfoRepository;

    private SimInfo buildTestSim(String iccid) {
        return SimInfo.builder()
                .iccid(iccid)
                .imsi("460001234567890")
                .msisdn("8613800138000")
                .sourceMno("MANUAL")
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
    @DisplayName("保存并查询SIM信息")
    void saveAndGetByIccid() {
        SimInfo simInfo = buildTestSim("99990000000000000001");

        int rows = simInfoRepository.save(simInfo);
        assertEquals(1, rows);

        SimInfo found = simInfoRepository.getByIccid("99990000000000000001");
        assertNotNull(found);
        assertEquals("99990000000000000001", found.getIccid());
        assertEquals("460001234567890", found.getImsi());
        assertEquals("8613800138000", found.getMsisdn());
        assertEquals("MANUAL", found.getSourceMno());
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedTime());
    }

    @Test
    @DisplayName("ICCID存在性检查")
    void existsByIccid() {
        assertFalse(simInfoRepository.existsByIccid("99990000000000000002"));

        simInfoRepository.save(buildTestSim("99990000000000000002"));

        assertTrue(simInfoRepository.existsByIccid("99990000000000000002"));
    }

    @Test
    @DisplayName("查询不存在的ICCID返回null")
    void getByIccid_notFound() {
        assertNull(simInfoRepository.getByIccid("99999999999999999999"));
    }

    @Test
    @DisplayName("更新SIM信息")
    void update() {
        SimInfo simInfo = buildTestSim("99990000000000000003");
        simInfoRepository.save(simInfo);

        SimInfo saved = simInfoRepository.getByIccid("99990000000000000003");
        saved.setImsi("460009999999999");
        saved.setMsisdn("8613900000000");
        simInfoRepository.update(saved);

        SimInfo updated = simInfoRepository.getByIccid("99990000000000000003");
        assertEquals("460009999999999", updated.getImsi());
        assertEquals("8613900000000", updated.getMsisdn());
    }

    @Test
    @DisplayName("Upsert - 新ICCID插入")
    void upsert_insert() {
        SimInfo simInfo = buildTestSim("99990000000000000004");
        simInfoRepository.upsertSimInfo(simInfo);

        SimInfo found = simInfoRepository.getByIccid("99990000000000000004");
        assertNotNull(found);
        assertEquals("MANUAL", found.getSourceMno());
    }

    @Test
    @DisplayName("Upsert - 已存在ICCID更新")
    void upsert_update() {
        SimInfo simInfo = buildTestSim("99990000000000000005");
        simInfoRepository.save(simInfo);

        SimInfo updated = buildTestSim("99990000000000000005");
        updated.setImsi("460008888888888");
        updated.setSourceMno("CUCC");
        updated.setSourceType("cucc_push");
        simInfoRepository.upsertSimInfo(updated);

        SimInfo found = simInfoRepository.getByIccid("99990000000000000005");
        assertEquals("460008888888888", found.getImsi());
        assertEquals("CUCC", found.getSourceMno());
        assertEquals("cucc_push", found.getSourceType());
    }

    @Test
    @DisplayName("ICCID唯一键约束")
    void save_duplicateIccid() {
        simInfoRepository.save(buildTestSim("99990000000000000006"));

        assertThrows(Exception.class, () -> {
            simInfoRepository.save(buildTestSim("99990000000000000006"));
        });
    }
}
