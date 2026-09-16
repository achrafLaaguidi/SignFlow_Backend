package com.laaguidi.SignFlow.controller.central;

import com.laaguidi.SignFlow.model.central.dto.response.AuditLogResponse;
import com.laaguidi.SignFlow.model.central.dto.response.PlatformMetricsResponse;
import com.laaguidi.SignFlow.service.central.CentralMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/central/monitoring")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class CentralMonitoringController {

    private final CentralMonitoringService monitoringService;

    /**
     * Get aggregate statistics (Metrics) for the Central Dashboard cards.
     * GET /api/v1/central/monitoring/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<PlatformMetricsResponse> getPlatformMetrics() {
        return ResponseEntity.ok(monitoringService.getPlatformMetrics());
    }

    /**
     * Get paginated system audit logs.
     * GET /api/v1/central/monitoring/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<PagedModel<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String query,
            @PageableDefault(page = 0, size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(monitoringService.getAuditLogs(query, pageable));
    }
}