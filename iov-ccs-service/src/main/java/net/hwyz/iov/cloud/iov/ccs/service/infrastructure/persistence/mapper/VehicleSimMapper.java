package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.VehicleSimPo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 车卡关联表 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleSimMapper extends BaseDao<VehicleSimPo, Long> {

    /**
     * 根据ICCID查询车卡关联
     *
     * @param iccid ICCID
     * @return 车卡关联
     */
    VehicleSimPo selectByIccid(String iccid);

    /**
     * 根据VIN查询车卡关联列表（一车双卡）
     *
     * @param vin VIN
     * @return 车卡关联列表
     */
    List<VehicleSimPo> selectByVin(String vin);

    /**
     * 根据ICCID判断是否存在
     *
     * @param iccid ICCID
     * @return 是否存在
     */
    boolean existsByIccid(String iccid);

    /**
     * 根据绑定状态查询列表
     *
     * @param bindingStatus 绑定状态
     * @return 车卡关联列表
     */
    List<VehicleSimPo> selectByBindingStatus(Integer bindingStatus);

    /**
     * 查询所有车卡关联（用于对账）
     *
     * @return 车卡关联列表
     */
    List<VehicleSimPo> selectAll();
}
