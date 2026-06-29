package net.hwyz.iov.cloud.iov.ccs.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM 状态变更事件
 * <p>
 * CCS 作为契约 owner 对外发布的版本化事件（Kafka，Key=VIN）
 * sim_status / realname_status 迁移时发布
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimStatusChangeEvent {

    /**
     * 事件版本（schema version）
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * VIN（可能为 null，如果 SIM 未绑定车辆）
     */
    private String vin;

    /**
     * ICCID
     */
    private String iccid;

    /**
     * SIM 状态：TEST=1, FORMAL=2, ...
     */
    private Integer simStatus;

    /**
     * 实名状态：NO_REAL_NAME=1, REAL_NAMED=2, ...
     */
    private Integer realnameStatus;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;
}
