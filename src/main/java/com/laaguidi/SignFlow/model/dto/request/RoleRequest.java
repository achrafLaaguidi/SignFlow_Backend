package com.laaguidi.SignFlow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    @NotEmpty(message = "Role must have at least one permission")
    private Set<UUID> permissionIds;
}
