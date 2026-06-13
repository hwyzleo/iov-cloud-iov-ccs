package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM导入候选持久化对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimImportCandidatePo {

    /**
     * PK
     */
    private Long id;

    /**
     * 批次类型：CMCC/CUCC
     */
    private String batchType;

    /**
     * 批次号：fileId 或 batchNo
     */
    private String batchNo;

    /**
     * 来源运营商：CMCC/CUCC
     */
    private String sourceMno;

    /**
     * ICCID
     */
    private String iccid;

    /**
     * IMSI
     */
    private String imsi;

    /**
     * MSISDN（规范化后）
     */
    private String msisdn;

    /**
     * 解析状态：OK/INVALID
     */
    private String parseStatus;

    /**
     * 入库状态：PENDING/SUCCESS/DUPLICATE/FAILED
     */
    private String storeStatus;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
