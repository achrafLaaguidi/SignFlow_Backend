package com.laaguidi.SignFlow.model.central.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlatformMetricsResponse {
    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long totalSuperAdmins;
}