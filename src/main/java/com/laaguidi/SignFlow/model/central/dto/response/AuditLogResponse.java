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
public class AuditLogResponse {
    private UUID id;
    private String action;
    private String performedBy;
    private String targetResource;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}