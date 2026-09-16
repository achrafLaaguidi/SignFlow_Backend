package com.laaguidi.SignFlow.model.central.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}