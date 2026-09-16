package com.laaguidi.SignFlow.config.tenant;

import com.laaguidi.SignFlow.security.JwtService; // أعمل Import لـ JwtService ديالك
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantId = null;

        String headerTenantId = request.getHeader("X-Tenant-ID");
        if (StringUtils.hasText(headerTenantId)) {
            tenantId = headerTenantId;
        }

        if (StringUtils.hasText(tenantId)) {
            String schemaName = tenantId.startsWith("tenant_") ? tenantId : "tenant_" + tenantId;
            TenantContext.setCurrentTenant(schemaName);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}