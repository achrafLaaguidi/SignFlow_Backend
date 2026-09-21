package com.laaguidi.SignFlow.model.central.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponse {
    private String id;
    private String companyName;
    private String adminEmail;
    private String status;
    private LocalDateTime createdAt;
}