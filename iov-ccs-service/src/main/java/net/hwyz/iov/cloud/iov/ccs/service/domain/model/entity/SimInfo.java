package net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM信息实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimInfo {

    /**
     * PK
     */
    private Long id;

    /**
     * ICCID（唯一键）
     */
    private String iccid;

    /**
     * IMSI
     */
    private String imsi;

    /**
     * MSISDN（入库前统一为86+号码）
     */
    private String msisdn;

    /**
     * 来源运营商：CMCC/CUCC/MANUAL
     */
    private String sourceMno;

    /**
     * 来源类型：cmcc_file/cucc_push/manual_save/manual_batch/sync_data
     */
    private String sourceType;

    /**
     * 来源引用：fileId/batchNo/操作单号等
     */
    private String sourceRef;

    /**
     * SIM状态：TEST=1
     */
    private Integer simStatus;

    /**
     * 绑定状态：UNBOUNDED=0
     */
    private Integer bindingStatus;

    /**
     * 实名状态：NO_REAL_NAME=1
     */
    private Integer realnameStatus;

    /**
     * 短信能力开关
     */
    private Boolean smsStatus;

    /**
     * 数据能力开关
     */
    private Boolean dataStatus;

    /**
     * 语音能力开关
     */
    private Boolean voiceStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
