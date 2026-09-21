package com.laaguidi.SignFlow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private UUID roleId;
}
