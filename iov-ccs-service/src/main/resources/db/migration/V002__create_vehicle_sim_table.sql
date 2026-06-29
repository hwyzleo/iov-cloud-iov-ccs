-- ============================================================
-- CCS-DSN-CR-002: 车卡关联表 DDL
-- 创建 tb_vehicle_sim 表
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_vehicle_sim (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  vin             VARCHAR(32)  NOT NULL COMMENT 'VIN',
  iccid           VARCHAR(32)  NOT NULL COMMENT 'ICCID',
  card_slot       TINYINT      NOT NULL COMMENT '卡槽：1=iccid1, 2=iccid2',
  binding_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '绑定状态：UNBOUNDED=0/BOUNDED=1',
  package_type    VARCHAR(16)  NULL COMMENT '套餐档：TEST/FORMAL（本期仅 TEST）',
  tbox_sn         VARCHAR(64)  NULL COMMENT 'TBOX 序列号（反查/审计）',
  source_event_id VARCHAR(64)  NULL COMMENT '来源事件 bindingId',
  source_seq      BIGINT       NULL COMMENT '来源事件 seq（乱序判定）',
  bound_time      DATETIME     NULL COMMENT '绑定时间',
  created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iccid (iccid),
  KEY idx_vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车卡关联（VIN↔ICCID）';

-- ============================================================
-- 说明：
-- 1. uk_iccid: 一张卡只能绑一车（一卡一车）
-- 2. idx_vin: 一车双卡（iccid1/iccid2）
-- 3. card_slot: 区分卡槽，1=iccid1, 2=iccid2
-- 4. binding_status: 0=未绑定, 1=已绑定
-- 5. package_type: 本期仅支持 TEST，后续扩展 FORMAL
-- 6. source_event_id/source_seq: 用于幂等和乱序判定
-- ============================================================
