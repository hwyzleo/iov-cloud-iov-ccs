package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;

/**
 * SIM信息服务接口
 * <p>
 * 负责SIM数据的入库操作
 *
 * @author hwyz_leo
 */
public interface SimInfoService {

    /**
     * 处理SIM存储事件
     * <p>
     * 监听SimStorageEvent事件，遍历SIM列表，对每个ICCID检查是否已存在：
     * - 已存在：按策略处理（CMCC/CUCC幂等跳过，MANUAL拒绝，sync-data更新）
     * - 不存在：插入tb_sim_info表
     *
     * @param event 存储事件
     */
    void handleStorageEvent(SimStorageEvent event);

    /**
     * 保存SIM信息
     *
     * @param simInfo SIM信息
     * @return 是否成功（false表示ICCID已存在）
     */
    boolean saveSimInfo(net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo simInfo);

    /**
     * 更新SIM信息（Upsert语义）
     *
     * @param simInfo SIM信息
     */
    void upsertSimInfo(net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo simInfo);
}
