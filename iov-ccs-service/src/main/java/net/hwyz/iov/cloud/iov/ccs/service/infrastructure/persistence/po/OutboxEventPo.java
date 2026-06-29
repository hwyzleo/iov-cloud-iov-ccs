package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox 事件持久化对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventPo {

    /**
     * PK
     */
    private Long id;

    /**
     * 事件类型：CARD_BINDING_STATUS / SIM_STATUS
     */
    private String eventType;

    /**
     * 聚合根 ID（VIN 或 ICCID）
     */
    private String aggregateId;

    /**
     * 事件 payload（JSON）
     */
    private String payload;

    /**
     * 状态：PENDING / PUBLISHED / FAILED
     */
    private String status;

    /**
     * 目标 topic
     */
    private String topic;

    /**
     * 消息 key
     */
    private String messageKey;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 发布时间
     */
    private LocalDateTime publishedTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
