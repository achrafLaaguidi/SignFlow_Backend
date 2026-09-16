package com.laaguidi.SignFlow.model.central.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "central_audit_logs")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CentralAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "action", nullable = false, length = 100)
    private String action; // e.g., "TENANT_CREATED", "TENANT_SUSPENDED", "ADMIN_LOGIN"

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy; // Email of the SuperAdmin

    @Column(name = "target_resource", length = 100)
    private String targetResource; // e.g., "tenant:ocp_group"

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Extra info (JSON or String)

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}