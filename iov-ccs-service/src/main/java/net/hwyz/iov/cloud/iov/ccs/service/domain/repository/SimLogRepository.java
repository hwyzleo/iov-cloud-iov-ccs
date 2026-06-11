package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimLog;

import java.util.List;

/**
 * SIM卡变更日志仓储接口
 *
 * @author hwyz_leo
 */
public interface SimLogRepository {

    /**
     * 保存变更日志
     *
     * @param simLog 变更日志实体
     * @return 影响行数
     */
    int save(SimLog simLog);

    /**
     * 根据ICCID删除变更日志
     *
     * @param iccid ICCID
     * @return 影响行数
     */
    int deleteByIccid(String iccid);

    /**
     * 根据ICCID查询变更日志列表
     *
     * @param iccid ICCID
     * @return 变更日志列表
     */
    List<SimLog> listByIccid(String iccid);
}
