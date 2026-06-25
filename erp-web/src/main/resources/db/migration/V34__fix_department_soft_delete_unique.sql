-- ============================================================================
-- V34: 修复部门软删除后无法重新创建同名部门的问题
-- 将 dept_code 上的唯一索引改为 (dept_code, deleted) 联合唯一索引，
-- 这样 deleted=1 的已删除记录不会与 deleted=0 的未删除记录冲突。
-- ============================================================================

ALTER TABLE sys_department
    DROP INDEX dept_code;

ALTER TABLE sys_department
    ADD UNIQUE INDEX uk_dept_code_deleted (dept_code, deleted);
