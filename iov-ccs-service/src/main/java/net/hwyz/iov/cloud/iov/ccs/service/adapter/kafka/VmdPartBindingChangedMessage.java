package net.hwyz.iov.cloud.iov.ccs.service.adapter.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * VMD车辆零件绑定变更事件Kafka消息
 * <p>
 * 对应VMD发布的 VehiclePartBindingChangedEvent（topic: vmd.vehicle-part-binding.changed），
 * 字段结构与 TSP 侧 VehiclePartBindingChangedEvent 保持一致。
 * 仅声明 CCS 关心的字段，replay/batchNum 等附加字段忽略。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VmdPartBindingChangedMessage {

    /**
     * VIN
     */
    private String vin;

    /**
     * 绑定ID（幂等键的一部分）
     */
    private Long bindingId;

    /**
     * 零件编码
     */
    private String partCode;

    /**
     * 零件序列号（TBOX场景即TBOX SN）
     */
    private String sn;

    /**
     * 设备类别：TBOX/DCU/CCU等
     */
    private String deviceCategory;

    /**
     * 车辆节点编码
     */
    private String vehicleNodeCode;

    /**
     * ICCID1（卡槽1）
     */
    private String iccid1;

    /**
     * ICCID2（卡槽2）
     */
    private String iccid2;

    /**
     * 变更类型：BIND/UNBIND/REPLACE
     */
    private String changeType;

    /**
     * REPLACE时的原绑定ID
     */
    private Long replaceOfBindingId;

    /**
     * 事件发生时间（UTC）
     */
    private Instant occurredAt;

    /**
     * 事件序列号（乱序判定）
     */
    private Long seq;
}
