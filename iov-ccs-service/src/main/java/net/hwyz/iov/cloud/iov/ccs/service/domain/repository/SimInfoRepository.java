package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

/**
 * SIM信息仓储接口
 *
 * @author hwyz_leo
 */
public interface SimInfoRepository {

    /**
     * 根据ICCID查询SIM信息
     *
     * @param iccid ICCID
     * @return SIM信息
     */
    SimInfo getByIccid(String iccid);

    /**
     * 根据ICCID判断是否存在
     *
     * @param iccid ICCID
     * @return 是否存在
     */
    boolean existsByIccid(String iccid);

    /**
     * 保存SIM信息
     *
     * @param simInfo SIM信息
     * @return 影响行数
     */
    int save(SimInfo simInfo);

    /**
     * 更新SIM信息
     *
     * @param simInfo SIM信息
     * @return 影响行数
     */
    int update(SimInfo simInfo);
}
