package net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CMCC文件请求记录实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmccFileRequestRecord {

    /**
     * PK
     */
    private Long id;

    /**
     * CMCC fileId（唯一）
     */
    private String fileId;

    /**
     * 同步开始时间
     */
    private LocalDateTime requestStart;

    /**
     * 同步结束时间
     */
    private LocalDateTime requestEnd;

    /**
     * timestamp（用于解密key派生）
     */
    private String ts;

    /**
     * 是否加密：0/1
     */
    private Integer encrypted;

    /**
     * 状态：APPLYING/NOTIFIED/DOWNLOADED/DECRYPTED/PARSED/STORED/FAILED
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 失败阶段
     */
    private String failureStage;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 解析总数
     */
    private Integer parsedTotal;

    /**
     * 入库成功数
     */
    private Integer storedSuccess;

    /**
     * 幂等重复数
     */
    private Integer storedDuplicate;

    /**
     * 入库失败数
     */
    private Integer storedFailed;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
