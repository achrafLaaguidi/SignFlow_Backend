package com.laaguidi.SignFlow.security;

import com.laaguidi.SignFlow.config.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService tenantUserDetailsService;
    private final UserDetailsService superAdminUserDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Qualifier("tenantUserDetailsService") UserDetailsService tenantUserDetailsService,
            @Qualifier("superAdminUserDetailsService") UserDetailsService superAdminUserDetailsService) {
        this.jwtService = jwtService;
        this.tenantUserDetailsService = tenantUserDetailsService;
        this.superAdminUserDetailsService = superAdminUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/refresh") ||
                path.startsWith("/api/v1/central/auth/login") ||
                path.startsWith("/api/v1/central/auth/refresh");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null) {
                String tenantId = jwtService.extractClaim(jwt, claims -> claims.get("tenantId", String.class));
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.setCurrentTenant(tenantId);
                    log.debug("TenantContext set to: {}", tenantId);
                }

                String userEmail = jwtService.extractUsername(jwt);

                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails;
                    if ("CENTRAL".equals(tenantId) || request.getRequestURI().startsWith("/api/v1/central")) {
                        userDetails = this.superAdminUserDetailsService.loadUserByUsername(userEmail);
                    } else {
                        userDetails = this.tenantUserDetailsService.loadUserByUsername(userEmail);
                    }

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities() // 👈 التأكد من تمرير السلطات صحيحة
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("JWT authentication processing failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            boolean isCentralRequest = request.getRequestURI().startsWith("/api/v1/central");
            String cookieName = isCentralRequest ? "central_access_token" : "access_token";

            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}