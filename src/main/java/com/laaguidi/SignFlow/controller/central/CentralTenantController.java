package com.laaguidi.SignFlow.controller.central;

import com.laaguidi.SignFlow.model.dto.request.TenantRequest;
import com.laaguidi.SignFlow.model.central.dto.request.TenantStatusRequest;
import com.laaguidi.SignFlow.model.dto.response.TenantResponse;
import com.laaguidi.SignFlow.service.central.SuperAdminAuthService;
import com.laaguidi.SignFlow.service.central.TenantProvisioningService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/central/tenants")
@RequiredArgsConstructor
public class CentralTenantController {

    private final TenantProvisioningService tenantProvisioningService;
    private final SuperAdminAuthService superAdminAuthService;

    /**
     * Provisions a new Tenant.
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody TenantRequest request
    ) {
        TenantResponse response = tenantProvisioningService.createTenantWithAdmin(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Gets a paginated list of all registered tenants.
     */
    @GetMapping
    public ResponseEntity<PagedModel<TenantResponse>> getAllTenants(
            @RequestParam(required = false) String query,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(tenantProvisioningService.getAllTenants(query, pageable));
    }

    /**
     * Gets details of a specific tenant by tenantId.
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenantById(
            @PathVariable UUID tenantId
    ) {
        return ResponseEntity.ok(tenantProvisioningService.getTenantById(tenantId));
    }

    /**
     * Updates tenant status (Active, Suspended, etc.).
     */
    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<TenantResponse> updateTenantStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantStatusRequest request
    ) {
        return ResponseEntity.ok(tenantProvisioningService.updateTenantStatus(tenantId, request));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        tenantProvisioningService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Impersonates a specific tenant and returns its full details.
     */
    @PostMapping("/{tenantId}/impersonate")
    public ResponseEntity<TenantResponse> impersonateTenant(
            @PathVariable UUID tenantId,
            HttpServletResponse response
    ) throws BadRequestException {
        superAdminAuthService.impersonateTenant(tenantId, response);

        TenantResponse tenantResponse = tenantProvisioningService.getTenantById(tenantId);
        return ResponseEntity.ok(tenantResponse);
    }

    /**
     * Exits impersonation and clears the tenant access token cookie.
     */
    @PostMapping("/exit-impersonation")
    public ResponseEntity<Void> exitImpersonation(HttpServletResponse response) {
        superAdminAuthService.exitImpersonation(response);
        return ResponseEntity.noContent().build();
    }
}