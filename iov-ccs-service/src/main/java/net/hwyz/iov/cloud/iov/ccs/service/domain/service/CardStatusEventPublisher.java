package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

import net.hwyz.iov.cloud.iov.ccs.service.domain.event.CardBindingStatusEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStatusChangeEvent;

/**
 * 卡状态事件发布器接口
 * <p>
 * CCS 作为契约 owner 对外发布版本化事件（Kafka，Key=VIN）
 * 采用 Outbox Pattern 保证至少一次投递
 *
 * @author hwyz_leo
 */
public interface CardStatusEventPublisher {

    /**
     * 发布车卡绑定状态变更事件
     * <p>
     * binding_status（UNBOUNDED↔BOUNDED）迁移时发布
     * payload：vin / iccid / card_slot / binding_status / source_seq / version
     *
     * @param event 车卡绑定状态变更事件
     */
    void publishBindingStatus(CardBindingStatusEvent event);

    /**
     * 发布 SIM 状态变更事件
     * <p>
     * sim_status / realname_status 迁移时发布
     * payload：vin / iccid / sim_status / realname_status / version
     *
     * @param event SIM 状态变更事件
     */
    void publishSimStatus(SimStatusChangeEvent event);

    /**
     * 重试发布失败的事件
     * <p>
     * 定时任务调用，扫描 outbox 中 FAILED 状态且可重试的事件
     *
     * @return 重试成功的事件数
     */
    int retryFailedEvents();

    /**
     * 发布待处理的事件
     * <p>
     * 定时任务调用，扫描 outbox 中 PENDING 状态的事件
     *
     * @return 发布成功的事件数
     */
    int publishPendingEvents();
}
