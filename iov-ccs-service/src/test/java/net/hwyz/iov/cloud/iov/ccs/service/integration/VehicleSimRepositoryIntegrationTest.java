package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.VehicleSimRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车卡关联仓储集成测试
 * <p>
 * 直接覆盖 {@link net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository.VehicleSimRepositoryImpl}
 * 的 insertPo / updatePo / upsert(insert/update) 路径。
 * 该类问题（Mapper 继承 BaseDao 但 XML 缺少 insertPo/updatePo）曾导致
 * {@code Invalid bound statement (not found): VehicleSimMapper.insertPo}，本测试用于回归防护。
 *
 * @author hwyz_leo
 */
@DisplayName("车卡关联仓储集成测试")
class VehicleSimRepositoryIntegrationTest extends BaseTest {

    /**
     * 测试用绑定状态常量，与 tb_vehicle_sim.binding_status 对齐
     */
    private static final int UNBOUNDED = 0;
    private static final int BOUND = 1;
    private static final String PACKAGE_TYPE_TEST = "TEST";

    @Autowired
    private VehicleSimRepository vehicleSimRepository;

    private VehicleSim buildTestVehicleSim(String vin, String iccid, int cardSlot, int bindingStatus) {
        return VehicleSim.builder()
                .vin(vin)
                .iccid(iccid)
                .cardSlot(cardSlot)
                .bindingStatus(bindingStatus)
                .packageType(PACKAGE_TYPE_TEST)
                .tboxSn("TBOX-SN-" + iccid)
                .sourceEventId("1")
                .sourceSeq(100L)
                .boundTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("保存并查询车卡关联（insertPo）")
    void saveAndGetByIccid() {
        VehicleSim vehicleSim = buildTestVehicleSim("HWYZTESTINT0000001", "99960000000000000001", 1, BOUND);

        int rows = vehicleSimRepository.save(vehicleSim);
        assertEquals(1, rows);

        VehicleSim found = vehicleSimRepository.getByIccid("99960000000000000001");
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals("HWYZTESTINT0000001", found.getVin());
        assertEquals("99960000000000000001", found.getIccid());
        assertEquals(1, found.getCardSlot());
        assertEquals(BOUND, found.getBindingStatus());
        assertEquals(PACKAGE_TYPE_TEST, found.getPackageType());
        assertNotNull(found.getBoundTime());
        assertNotNull(found.getCreatedTime());
    }

    @Test
    @DisplayName("更新车卡关联（updatePo）")
    void update() {
        vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000002", "99960000000000000002", 1, UNBOUNDED));

        VehicleSim saved = vehicleSimRepository.getByIccid("99960000000000000002");
        saved.setBindingStatus(BOUND);
        saved.setTboxSn("TBOX-SN-UPDATED");
        saved.setSourceEventId("9");
        saved.setSourceSeq(900L);
        int rows = vehicleSimRepository.update(saved);
        assertEquals(1, rows);

        VehicleSim updated = vehicleSimRepository.getByIccid("99960000000000000002");
        assertEquals(BOUND, updated.getBindingStatus());
        assertEquals("TBOX-SN-UPDATED", updated.getTboxSn());
        assertEquals("9", updated.getSourceEventId());
        assertEquals(900L, updated.getSourceSeq());
    }

    @Test
    @DisplayName("Upsert - 新ICCID插入")
    void upsert_insert() {
        VehicleSim vehicleSim = buildTestVehicleSim("HWYZTESTINT0000003", "99960000000000000003", 2, BOUND);

        vehicleSimRepository.upsert(vehicleSim);

        VehicleSim found = vehicleSimRepository.getByIccid("99960000000000000003");
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals("HWYZTESTINT0000003", found.getVin());
        assertEquals(2, found.getCardSlot());
        assertEquals(BOUND, found.getBindingStatus());
        assertEquals("1", found.getSourceEventId());
        assertEquals(100L, found.getSourceSeq());
    }

    @Test
    @DisplayName("Upsert - 已存在ICCID更新且不产生重复行")
    void upsert_update() {
        String vin = "HWYZTESTINT0000004";
        String iccid = "99960000000000000004";
        vehicleSimRepository.save(buildTestVehicleSim(vin, iccid, 1, UNBOUNDED));

        VehicleSim updated = VehicleSim.builder()
                .vin(vin)
                .iccid(iccid)
                .cardSlot(1)
                .bindingStatus(BOUND)
                .packageType(PACKAGE_TYPE_TEST)
                .tboxSn("TBOX-SN-NEW")
                .sourceEventId("2")
                .sourceSeq(200L)
                .boundTime(LocalDateTime.now())
                .build();
        vehicleSimRepository.upsert(updated);

        VehicleSim found = vehicleSimRepository.getByIccid(iccid);
        assertEquals(BOUND, found.getBindingStatus());
        assertEquals("TBOX-SN-NEW", found.getTboxSn());
        assertEquals("2", found.getSourceEventId());
        assertEquals(200L, found.getSourceSeq());

        // 确认是更新而非重复插入
        List<VehicleSim> byVin = vehicleSimRepository.listByVin(vin);
        assertEquals(1, byVin.size(), "upsert 已存在 ICCID 时不应插入重复行");
    }

    @Test
    @DisplayName("ICCID存在性检查")
    void existsByIccid() {
        assertFalse(vehicleSimRepository.existsByIccid("99960000000000000005"));

        vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000005", "99960000000000000005", 1, BOUND));

        assertTrue(vehicleSimRepository.existsByIccid("99960000000000000005"));
    }

    @Test
    @DisplayName("根据VIN查询一车双卡并按卡槽排序")
    void listByVin() {
        String vin = "HWYZTESTINT0000006";
        // 故意先插卡槽2，再插卡槽1，验证排序
        vehicleSimRepository.save(buildTestVehicleSim(vin, "99960000000000000007", 2, BOUND));
        vehicleSimRepository.save(buildTestVehicleSim(vin, "99960000000000000006", 1, BOUND));

        List<VehicleSim> list = vehicleSimRepository.listByVin(vin);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getCardSlot());
        assertEquals(2, list.get(1).getCardSlot());
    }

    @Test
    @DisplayName("根据绑定状态查询")
    void listByBindingStatus() {
        String iccid = "99960000000000000008";
        vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000007", iccid, 1, BOUND));

        List<VehicleSim> list = vehicleSimRepository.listByBindingStatus(BOUND);
        assertTrue(list.stream().anyMatch(v -> iccid.equals(v.getIccid())));
    }

    @Test
    @DisplayName("查询所有车卡关联包含新建记录")
    void listAll_containsSaved() {
        String iccid = "99960000000000000009";
        vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000008", iccid, 1, BOUND));

        List<VehicleSim> list = vehicleSimRepository.listAll();
        assertTrue(list.stream().anyMatch(v -> iccid.equals(v.getIccid())));
    }

    @Test
    @DisplayName("ICCID唯一键约束")
    void save_duplicateIccid() {
        String iccid = "99960000000000000010";
        vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000009", iccid, 1, BOUND));

        assertThrows(Exception.class, () ->
                vehicleSimRepository.save(buildTestVehicleSim("HWYZTESTINT0000010", iccid, 2, BOUND)));
    }
}
