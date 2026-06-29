package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.OutboxEvent;

import java.util.List;

/**
 * Outbox 事件仓储接口
 *
 * @author hwyz_leo
 */
public interface OutboxEventRepository {

    /**
     * 保存 outbox 事件
     *
     * @param event outbox 事件
     * @return 影响行数
     */
    int save(OutboxEvent event);

    /**
     * 更新 outbox 事件
     *
     * @param event outbox 事件
     * @return 影响行数
     */
    int update(OutboxEvent event);

    /**
     * 查询待发布的事件
     *
     * @param limit 限制数量
     * @return 待发布事件列表
     */
    List<OutboxEvent> listPending(int limit);

    /**
     * 查询失败且可重试的事件
     *
     * @param limit 限制数量
     * @return 失败事件列表
     */
    List<OutboxEvent> listRetryable(int limit);

    /**
     * 根据 ID 查询
     *
     * @param id 主键 ID
     * @return outbox 事件
     */
    OutboxEvent getById(Long id);
}
