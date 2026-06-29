package net.hwyz.iov.cloud.iov.ccs.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车卡绑定状态变更事件
 * <p>
 * CCS 作为契约 owner 对外发布的版本化事件（Kafka，Key=VIN）
 * binding_status（UNBOUNDED↔BOUNDED）迁移时发布
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBindingStatusEvent {

    /**
     * 事件版本（schema version）
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * VIN
     */
    private String vin;

    /**
     * ICCID
     */
    private String iccid;

    /**
     * 卡槽：1=iccid1, 2=iccid2
     */
    private Integer cardSlot;

    /**
     * 绑定状态：UNBOUNDED=0 / BOUNDED=1
     */
    private Integer bindingStatus;

    /**
     * 来源事件 seq（乱序判定）
     */
    private Long sourceSeq;

    /**
     * 来源事件 ID
     */
    private String sourceEventId;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;
}
