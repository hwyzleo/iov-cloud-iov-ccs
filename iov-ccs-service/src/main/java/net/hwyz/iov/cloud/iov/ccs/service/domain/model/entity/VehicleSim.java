package net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车卡关联实体（VIN↔ICCID）
 * <p>
 * 承载车辆与SIM卡的绑定关系，支持一车双卡（iccid1/iccid2）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSim {

    /**
     * PK
     */
    private Long id;

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
     * 绑定状态：UNBOUNDED=0/BOUNDED=1
     */
    private Integer bindingStatus;

    /**
     * 套餐档：TEST/FORMAL（本期仅 TEST）
     */
    private String packageType;

    /**
     * TBOX 序列号（反查/审计）
     */
    private String tboxSn;

    /**
     * 来源事件 bindingId
     */
    private String sourceEventId;

    /**
     * 来源事件 seq（乱序判定）
     */
    private Long sourceSeq;

    /**
     * 绑定时间
     */
    private LocalDateTime boundTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
