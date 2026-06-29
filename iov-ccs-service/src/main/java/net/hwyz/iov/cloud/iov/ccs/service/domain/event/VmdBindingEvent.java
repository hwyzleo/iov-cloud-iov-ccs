package net.hwyz.iov.cloud.iov.ccs.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VMD绑定事件
 * <p>
 * 当VMD发布VehiclePartBindingChangedEvent时，消费者解析后发布此事件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdBindingEvent {

    /**
     * 绑定ID（幂等键的一部分）
     */
    private String bindingId;

    /**
     * 变更类型：BIND/UNBIND
     */
    private String changeType;

    /**
     * VIN
     */
    private String vin;

    /**
     * TBOX序列号
     */
    private String tboxSn;

    /**
     * ICCD1（卡槽1）
     */
    private String iccid1;

    /**
     * ICCID2（卡槽2）
     */
    private String iccid2;

    /**
     * 事件序列号（乱序判定）
     */
    private Long seq;

    /**
     * 事件发生时间（乱序判定）
     */
    private LocalDateTime occurredAt;

    /**
     * 原始事件ID（用于幂等处理）
     */
    private String originalEventId;
}
