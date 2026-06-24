-- V3: Align sys_* tables with BaseEntity audit fields
-- BaseEntity requires: created_by, updated_by, version (logical-delete `deleted` 已有)
-- 修复 @ConditionalOnProperty 切换到 MysqlUserDetailsLoader 后暴露的列缺失问题

-- sys_user: 缺 updated_by + version
ALTER TABLE sys_user
  ADD COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER created_by,
  ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- sys_department: 缺 created_by + updated_by + version
ALTER TABLE sys_department
  ADD COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER updated_at,
  ADD COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER created_by,
  ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- sys_role: 缺 created_by + updated_by + version
ALTER TABLE sys_role
  ADD COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER updated_at,
  ADD COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER created_by,
  ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- sys_permission: 缺 created_by + updated_by + version
ALTER TABLE sys_permission
  ADD COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER updated_at,
  ADD COLUMN updated_by BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER created_by,
  ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';
