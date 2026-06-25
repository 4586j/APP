package com.erp.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssignUsersRequest {
    @NotNull
    private List<Long> userIds;
}
