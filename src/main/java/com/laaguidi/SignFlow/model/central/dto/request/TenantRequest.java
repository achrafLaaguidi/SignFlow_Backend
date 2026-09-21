package com.laaguidi.SignFlow.model.central.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantRequest {

    @NotBlank(message = "DB Schema is required")
    @Size(min = 3, max = 50, message = "DB Schema  must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "DB Schema  must contain only alphanumeric characters and underscores")
    private String dbSchema;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Admin full name is required")
    private String adminFullName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Email must be valid")
    private String adminEmail;

    @NotBlank(message = "Admin initial password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String adminPassword;
}