package com.erp.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户批量导入 Excel DTO。
 */
@Data
public class UserImportExcelDTO {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("部门名称")
    private String departmentName;

    @ExcelProperty("角色编码")
    private String roleCode;
}
