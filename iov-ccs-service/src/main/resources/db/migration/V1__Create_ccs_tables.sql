-- V1: 创建 CCS 核心表结构
-- SIM基础信息表、CMCC文件请求记录表、SIM导入候选表

CREATE TABLE IF NOT EXISTS tb_sim_info (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  iccid           VARCHAR(32)  NOT NULL COMMENT 'ICCID（唯一键）',
  imsi            VARCHAR(32)  NOT NULL COMMENT 'IMSI',
  msisdn          VARCHAR(32)  NOT NULL COMMENT 'MSISDN（入库前统一为86+号码）',
  source_mno      VARCHAR(16)  NOT NULL COMMENT '来源运营商：CMCC/CUCC/MANUAL',
  source_type     VARCHAR(32)  NOT NULL COMMENT '来源类型：cmcc_file/cucc_push/manual_save/manual_batch/sync_data',
  source_ref      VARCHAR(64)  NULL COMMENT '来源引用：fileId/batchNo/操作单号等',
  sim_status      TINYINT      NOT NULL DEFAULT 1 COMMENT 'SIM状态：TEST=1',
  binding_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '绑定状态：UNBOUNDED=0',
  realname_status TINYINT      NOT NULL DEFAULT 1 COMMENT '实名状态：NO_REAL_NAME=1',
  sms_status      TINYINT      NOT NULL DEFAULT 1 COMMENT '短信能力开关',
  data_status     TINYINT      NOT NULL DEFAULT 1 COMMENT '数据能力开关',
  voice_status    TINYINT      NOT NULL DEFAULT 1 COMMENT '语音能力开关',
  created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iccid (iccid),
  KEY idx_imsi (imsi),
  KEY idx_msisdn (msisdn),
  KEY idx_source (source_mno, source_type, source_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SIM基础信息';

CREATE TABLE IF NOT EXISTS tb_cmcc_file_request_record (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  file_id           VARCHAR(64)  NOT NULL COMMENT 'CMCC fileId（唯一）',
  request_start     DATETIME     NOT NULL COMMENT '同步开始时间',
  request_end       DATETIME     NOT NULL COMMENT '同步结束时间',
  ts                VARCHAR(64)  NOT NULL COMMENT 'timestamp（用于解密key派生）',
  encrypted         TINYINT      NOT NULL COMMENT '是否加密：0/1',
  status            VARCHAR(16)  NOT NULL COMMENT 'APPLYING/NOTIFIED/DOWNLOADED/DECRYPTED/PARSED/STORED/FAILED',
  retry_count       INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
  failure_stage     VARCHAR(16)  NULL COMMENT '失败阶段',
  failure_reason    VARCHAR(512) NULL COMMENT '失败原因',
  parsed_total      INT          NULL COMMENT '解析总数',
  stored_success    INT          NULL COMMENT '入库成功数',
  stored_duplicate  INT          NULL COMMENT '幂等重复数',
  stored_failed     INT          NULL COMMENT '入库失败数',
  created_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_id (file_id),
  KEY idx_status (status, updated_time),
  KEY idx_request_range (request_start, request_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMCC文件请求记录';

CREATE TABLE IF NOT EXISTS tb_sim_import_candidate (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  batch_type      VARCHAR(16)  NOT NULL COMMENT '批次类型：CMCC/CUCC',
  batch_no        VARCHAR(64)  NOT NULL COMMENT '批次号：fileId 或 batchNo',
  source_mno      VARCHAR(16)  NOT NULL COMMENT '来源运营商：CMCC/CUCC',
  iccid           VARCHAR(32)  NOT NULL COMMENT 'ICCID',
  imsi            VARCHAR(32)  NOT NULL COMMENT 'IMSI',
  msisdn          VARCHAR(32)  NOT NULL COMMENT 'MSISDN（规范化后）',
  parse_status    VARCHAR(16)  NOT NULL COMMENT '解析状态：OK/INVALID',
  store_status    VARCHAR(16)  NOT NULL COMMENT '入库状态：PENDING/SUCCESS/DUPLICATE/FAILED',
  failure_reason  VARCHAR(512) NULL COMMENT '失败原因',
  created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_iccid (batch_type, batch_no, iccid),
  KEY idx_store_status (store_status, updated_time),
  KEY idx_iccid (iccid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SIM导入候选集';
