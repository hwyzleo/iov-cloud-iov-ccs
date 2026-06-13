package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

import java.util.List;
import java.util.Map;

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

    /**
     * Upsert语义：ICCID不存在则插入，已存在则更新
     * <p>
     * 允许覆盖 IMSI/MSISDN/source_* 字段
     *
     * @param simInfo SIM信息
     */
    void upsertSimInfo(SimInfo simInfo);

    /**
     * 条件查询列表（支持分页）
     *
     * @param params 查询条件
     * @return SIM信息列表
     */
    List<SimInfo> listByCondition(Map<String, Object> params);

    /**
     * 根据ID删除SIM信息（物理删除）
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);
}
