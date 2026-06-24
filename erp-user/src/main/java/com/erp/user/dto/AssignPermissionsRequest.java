package com.erp.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 给角色绑定权限（整体替换语义）。 */
@Data
public class AssignPermissionsRequest {

    @NotNull(message = "permissionIds 不能为 null（清空请传空数组）")
    private List<Long> permissionIds;
}
