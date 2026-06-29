-- ============================================================
-- CCS-DSN-CR-002: Outbox 事件表 DDL
-- 创建 tb_outbox_event 表，用于保证事件至少一次投递
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_outbox_event (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  event_type      VARCHAR(32)  NOT NULL COMMENT '事件类型：CARD_BINDING_STATUS / SIM_STATUS',
  aggregate_id    VARCHAR(64)  NOT NULL COMMENT '聚合根 ID（VIN 或 ICCID）',
  payload         TEXT         NOT NULL COMMENT '事件 payload（JSON）',
  status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / PUBLISHED / FAILED',
  topic           VARCHAR(128) NOT NULL COMMENT '目标 topic',
  message_key     VARCHAR(64)  NULL COMMENT '消息 key',
  retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
  max_retry       INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  failure_reason  VARCHAR(512) NULL COMMENT '失败原因',
  created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  published_time  DATETIME     NULL COMMENT '发布时间',
  updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_aggregate (aggregate_id),
  KEY idx_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbox事件表（保证至少一次投递）';

-- ============================================================
-- 说明：
-- 1. 采用 Outbox Pattern 保证事件至少一次投递
-- 2. 绑定成功回写 tb_vehicle_sim 后在同一事务中写入 outbox 记录
-- 3. 定时任务扫描 PENDING 状态记录，发送到 Kafka 后更新为 PUBLISHED
-- 4. 发送失败的记录会重试，超过 max_retry 后标记为 FAILED 并告警
-- ============================================================
