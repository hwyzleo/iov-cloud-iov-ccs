package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

import java.util.List;
import java.util.Map;

/**
 * 手动录入SIM服务接口
 * <p>
 * 负责后台手动录入SIM卡信息
 *
 * @author hwyz_leo
 */
public interface ManualSimService {

    /**
     * 保存单条SIM信息
     * <p>
     * ICCID已存在时拒绝（抛出异常）
     *
     * @param simInfo SIM信息
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException ICCID已存在时抛出
     */
    void saveSimInfo(SimInfo simInfo);

    /**
     * 批量保存SIM信息
     * <p>
     * 复用单条逻辑，部分失败收集后整体抛出
     *
     * @param simInfoList SIM信息列表
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException 部分失败时抛出，包含失败ICCID列表
     */
    void batchSaveSimInfo(List<SimInfo> simInfoList);

    /**
     * 同步数据
     * <p>
     * Hex解码JSON，强制状态字段，Upsert语义
     *
     * @param hexData Hex编码的JSON数据
     */
    void syncData(String hexData);

    /**
     * 条件查询SIM信息列表（支持分页）
     *
     * @param params 查询条件
     * @return SIM信息列表
     */
    List<SimInfo> listSimInfo(Map<String, Object> params);

    /**
     * 根据ICCID获取SIM信息详情
     *
     * @param iccid ICCID
     * @return SIM信息
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException 不存在时抛出
     */
    SimInfo getSimInfo(String iccid);

    /**
     * 更新SIM信息（仅MANUAL来源可更新）
     *
     * @param iccid   ICCID
     * @param simInfo 待更新的SIM信息
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException 不存在或非MANUAL来源时抛出
     */
    void updateSimInfo(String iccid, SimInfo simInfo);

    /**
     * 删除SIM信息（物理删除+审计）
     *
     * @param iccid ICCID
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException 不存在时抛出
     */
    void deleteSimInfo(String iccid);

    /**
     * 批量删除SIM信息（物理删除+审计）
     *
     * @param iccids ICCID列表
     * @throws net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException 部分失败时抛出
     */
    void batchDeleteSimInfo(List<String> iccids);
}
