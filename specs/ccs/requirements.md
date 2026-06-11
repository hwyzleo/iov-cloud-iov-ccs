# CCS系统SIM卡模块 - Requirements

## 1. Overview
CCS系统SIM卡模块负责SIM卡的全生命周期管理，从TSP系统迁移而来，提供SIM卡信息管理、状态管理、能力管理、批量导入和变更日志功能。

## 2. Background & Goals

### 背景
- CCS（Connected Car Service）系统是车联网服务平台的独立子系统
- SIM卡管理功能原由TSP系统提供，需要完整迁移到CCS系统
- 迁移需保持功能一致性，确保业务连续性

### 目标
- 从TSP系统完整迁移SIM卡管理功能到CCS系统
- 采用独立数据源，与TSP系统数据隔离
- 提供完整的SIM卡生命周期管理能力
- 支持批量导入和变更日志记录

### 非目标（明确不做）
- 不实现TSP系统其他模块功能
- 不实现SIM卡与车辆的绑定关系管理
- 不实现SIM卡流量套餐管理
- 不实现SIM卡费用结算功能

## 3. User Stories

### US-001: 查询SIM卡信息
**As a** 运营商管理员, **I want** 查询SIM卡列表和详情, **so that** 了解SIM卡的使用状态和基本信息。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户提交分页查询请求 THE SYSTEM SHALL 按ICCID、创建时间等条件返回分页SIM卡列表
- WHEN 用户指定SIM卡ID THE SYSTEM SHALL 返回该SIM卡的详细信息
- WHEN 用户请求导出 THE SYSTEM SHALL 生成包含SIM卡信息的Excel文件
- WHILE 查询结果为空 THE SYSTEM SHALL 返回空列表，不报错
- IF 查询参数格式错误 THEN THE SYSTEM SHALL 返回参数校验失败提示

### US-002: 新增SIM卡
**As a** 运营商管理员, **I want** 手动录入SIM卡信息, **so that** 将新SIM卡纳入系统管理。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户提交SIM卡新增请求 THE SYSTEM SHALL 校验ICCID全局唯一性
- WHEN ICCID已存在 THE SYSTEM SHALL 返回重复错误提示
- WHEN 必填字段缺失 THE SYSTEM SHALL 返回字段校验失败提示
- WHEN 新增成功 THE SYSTEM SHALL 返回新创建的SIM卡ID
- WHILE 新增SIM卡 THE SYSTEM SHALL 默认状态设置为"测试"
- WHILE 新增SIM卡 THE SYSTEM SHALL 默认开启所有能力（短信、数据、语音）
- IF ICCID长度超过50字符 THEN THE SYSTEM SHALL 返回格式错误提示
- IF MSISDN长度超过20字符 THEN THE SYSTEM SHALL 返回格式错误提示

### US-003: 修改SIM卡信息
**As a** 运营商管理员, **I want** 修改SIM卡基本信息, **so that** 保持SIM卡信息的准确性。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户提交SIM卡修改请求 THE SYSTEM SHALL 校验SIM卡ID是否存在
- WHEN 用户修改ICCID THE SYSTEM SHALL 校验新ICCID的全局唯一性
- WHEN 修改成功 THE SYSTEM SHALL 返回更新后的SIM卡信息
- WHEN 修改完成 THE SYSTEM SHALL 自动记录变更日志
- IF SIM卡ID不存在 THEN THE SYSTEM SHALL 返回资源不存在错误
- IF 新ICCID与其他SIM卡重复 THEN THE SYSTEM SHALL 返回重复错误提示

### US-004: 删除SIM卡
**As a** 运营商管理员, **I want** 删除单个或多个SIM卡, **so that** 清理无效的SIM卡记录。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户提交单个SIM卡删除请求 THE SYSTEM SHALL 删除指定SIM卡记录
- WHEN 用户提交批量删除请求 THE SYSTEM SHALL 批量删除多个SIM卡记录
- WHEN SIM卡被删除 THE SYSTEM SHALL 同步删除相关变更日志
- IF SIM卡ID不存在 THEN THE SYSTEM SHALL 返回资源不存在错误
- IF 删除过程中发生异常 THEN THE SYSTEM SHALL 回滚所有删除操作

### US-005: 管理SIM卡状态
**As a** 运营商管理员, **I want** 管理SIM卡的生命周期状态, **so that** 跟踪SIM卡的使用阶段。

**Acceptance Criteria** (EARS 语法):
- WHEN SIM卡处于"测试"状态 THE SYSTEM SHALL 允许流转到"库存"状态
- WHEN SIM卡处于"库存"状态 THE SYSTEM SHALL 允许流转到"激活"状态
- WHEN 状态变更完成 THE SYSTEM SHALL 自动记录变更日志
- WHEN 状态变更完成 THE SYSTEM SHALL 自动更新修改时间
- WHILE 状态变更 THE SYSTEM SHALL 校验操作权限
- IF 状态流转不符合规则 THEN THE SYSTEM SHALL 返回状态流转错误提示

### US-006: 管理SIM卡能力
**As a** 运营商管理员, **I want** 管理SIM卡的功能能力, **so that** 控制SIM卡支持的服务类型。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户修改SIM卡能力 THE SYSTEM SHALL 支持短信、数据、语音三种能力的开关设置
- WHEN 能力变更完成 THE SYSTEM SHALL 自动记录变更日志
- WHILE 能力变更 THE SYSTEM SHALL 校验操作权限
- IF 能力参数格式错误 THEN THE SYSTEM SHALL 返回参数校验失败提示

### US-007: 批量导入SIM卡
**As a** 运营商管理员, **I want** 批量导入SIM卡信息, **so that** 快速录入大量SIM卡数据。

**Acceptance Criteria** (EARS 语法):
- WHEN 用户提交批量导入请求 THE SYSTEM SHALL 校验运营商类型参数
- WHEN 导入过程中遇到已存在的ICCID THE SYSTEM SHALL 自动跳过该记录
- WHEN 导入完成 THE SYSTEM SHALL 将新SIM卡状态设置为"测试"
- WHEN 导入完成 THE SYSTEM SHALL 为新SIM卡开启所有能力
- WHILE 导入处理 THE SYSTEM SHALL 记录导入日志
- IF 运营商类型参数缺失 THEN THE SYSTEM SHALL 返回参数校验失败提示
- IF SIM卡列表为空 THEN THE SYSTEM SHALL 返回空列表错误提示

## 4. Constraints & Assumptions

### 技术约束
- 后端框架：Spring Boot 3.x
- ORM框架：MyBatis
- 数据库：MySQL 8.0
- JDK版本：17

### 依赖
- CCS系统基础框架（用户认证、权限管理）
- 运营商编码配置数据
- Excel处理组件

### 前置条件
- CCS系统基础架构已搭建完成
- 数据库表结构已创建
- 权限配置体系已就绪

## 5. Out of Scope

以下功能明确不在本期范围内：

- SIM卡与车辆绑定关系管理
- SIM卡流量套餐管理
- SIM卡费用结算功能
- TSP系统其他模块迁移
- SIM卡实名认证功能
- SIM卡远程写卡功能
- 多运营商API对接

## 6. Changelog

| Date | Change ID | Type | Description |
|------|-----------|------|-------------|
| 2024-01-01 | CR-001 | Added | 初始需求文档创建 |
