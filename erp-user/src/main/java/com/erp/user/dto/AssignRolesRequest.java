package com.erp.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 给用户绑定角色（整体替换语义：传空清空）。 */
@Data
public class AssignRolesRequest {

    @NotNull(message = "roleIds 不能为 null（清空请传空数组）")
    private List<Long> roleIds;
}
