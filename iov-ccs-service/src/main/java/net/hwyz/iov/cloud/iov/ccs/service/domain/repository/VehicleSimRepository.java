package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;

import java.util.List;

/**
 * 车卡关联仓储接口
 *
 * @author hwyz_leo
 */
public interface VehicleSimRepository {

    /**
     * 根据ICCID查询车卡关联
     *
     * @param iccid ICCID
     * @return 车卡关联
     */
    VehicleSim getByIccid(String iccid);

    /**
     * 根据VIN查询车卡关联列表（一车双卡）
     *
     * @param vin VIN
     * @return 车卡关联列表
     */
    List<VehicleSim> listByVin(String vin);

    /**
     * 根据ICCID判断是否存在
     *
     * @param iccid ICCID
     * @return 是否存在
     */
    boolean existsByIccid(String iccid);

    /**
     * 保存车卡关联
     *
     * @param vehicleSim 车卡关联
     * @return 影响行数
     */
    int save(VehicleSim vehicleSim);

    /**
     * 更新车卡关联
     *
     * @param vehicleSim 车卡关联
     * @return 影响行数
     */
    int update(VehicleSim vehicleSim);

    /**
     * Upsert语义：ICCID不存在则插入，已存在则更新
     *
     * @param vehicleSim 车卡关联
     */
    void upsert(VehicleSim vehicleSim);

    /**
     * 根据绑定状态查询列表
     *
     * @param bindingStatus 绑定状态
     * @return 车卡关联列表
     */
    List<VehicleSim> listByBindingStatus(Integer bindingStatus);

    /**
     * 查询所有车卡关联（用于对账）
     *
     * @return 车卡关联列表
     */
    List<VehicleSim> listAll();
}
