package com.laaguidi.SignFlow.service.central;

import com.laaguidi.SignFlow.model.central.entity.CentralAuditLog;
import com.laaguidi.SignFlow.model.central.dto.response.AuditLogResponse;
import com.laaguidi.SignFlow.model.central.dto.response.PlatformMetricsResponse;
import com.laaguidi.SignFlow.model.dto.response.TenantResponse;
import com.laaguidi.SignFlow.model.mapper.central.CentralAuditLogsMapper;
import com.laaguidi.SignFlow.repository.central.CentralAuditLogRepository;
import com.laaguidi.SignFlow.repository.central.SuperAdminRepository;
import com.laaguidi.SignFlow.repository.central.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralMonitoringService {

    private final TenantRepository tenantRepository;
    private final SuperAdminRepository superAdminRepository;
    private final CentralAuditLogRepository auditLogRepository;
    private final CentralAuditLogsMapper  mapper;

    /**
     * Helper method to log events asynchronously without slowing down main requests.
     */
    @Async
    public void logEvent(String action, String performedBy, String targetResource, String details, String ipAddress) {
        try {
            CentralAuditLog logEntry = CentralAuditLog.builder()
                    .action(action)
                    .performedBy(performedBy)
                    .targetResource(targetResource)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save central audit log", e);
        }
    }

    /**
     * Calculates live platform statistics for dashboard metrics cards.
     */
    @Transactional(readOnly = true)
    public PlatformMetricsResponse getPlatformMetrics() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.findAll().stream()
                .filter(t -> "ACTIVE".equalsIgnoreCase(String.valueOf(t.getStatus()))).count();
        long suspendedTenants = tenantRepository.findAll().stream()
                .filter(t -> "SUSPENDED".equalsIgnoreCase(String.valueOf(t.getStatus()))).count();
        long totalSuperAdmins = superAdminRepository.count();

        return PlatformMetricsResponse.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .totalSuperAdmins(totalSuperAdmins)
                .build();
    }

    /**
     * Retrieves paginated audit logs.
     */
    @Transactional(readOnly = true)
    public PagedModel<AuditLogResponse> getAuditLogs(String query,Pageable pageable) {
        Page<CentralAuditLog> auditLogs=auditLogRepository.searchAuditLogs(query,pageable);
        List<AuditLogResponse> auditLogResponses= mapper.toAuditLogResponseList(auditLogs.getContent());
        Page<AuditLogResponse> responsePage = new PageImpl<>(
                auditLogResponses,
                auditLogs.getPageable(),
                auditLogs.getTotalElements()
        );
        return new PagedModel<>(responsePage);
    }
}