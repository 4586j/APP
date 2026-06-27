SET NAMES utf8mb4;

-- ============================================================================
-- V41: 迁移 dat_upload 已有数据到 dat_file
-- 每条记录转为根目录下的文件（parent_id = NULL）
-- 旧共享部门数据保留在 dat_upload_dept_share 中供参考
-- ============================================================================

INSERT INTO `dat_file` (
    `parent_id`, `is_directory`, `name`, `display_name`, `extension`,
    `file_size`, `storage_path`, `file_type`,
    `department`, `dept_id`, `row_count`, `parsed`, `remark`,
    `created_by`, `created_at`, `updated_at`, `deleted`
)
SELECT
    NULL,
    0,
    COALESCE(NULLIF(TRIM(original_name), ''), file_name),
    file_name,
    CASE
        WHEN file_name LIKE '%.%' THEN CONCAT('.', SUBSTRING_INDEX(file_name, '.', -1))
        ELSE NULL
    END,
    COALESCE(file_size, 0),
    file_path,
    file_type,
    `department`,
    dept_id,
    COALESCE(row_count, 0),
    parsed,
    remark,
    created_by,
    created_at,
    created_at,
    deleted
FROM `dat_upload`
WHERE deleted = 0;
