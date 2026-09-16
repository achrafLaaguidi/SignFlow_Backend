package com.laaguidi.SignFlow.service.central;

import com.laaguidi.SignFlow.exception.ConflictException;
import com.laaguidi.SignFlow.exception.TenantNotFoundException;
import com.laaguidi.SignFlow.exception.UnauthorizedException;
import com.laaguidi.SignFlow.model.central.dto.request.SuperAdminRegisterRequest;
import com.laaguidi.SignFlow.model.central.dto.response.SuperAdminResponse;
import com.laaguidi.SignFlow.model.central.entity.CentralRefreshToken;
import com.laaguidi.SignFlow.model.central.entity.SuperAdmin;
import com.laaguidi.SignFlow.model.central.entity.Tenant;
import com.laaguidi.SignFlow.model.dto.request.AuthRequest;
import com.laaguidi.SignFlow.model.enums.Status;
import com.laaguidi.SignFlow.model.mapper.central.SuperAdminMapper;
import com.laaguidi.SignFlow.repository.central.CentralRefreshTokenRepository;
import com.laaguidi.SignFlow.repository.central.SuperAdminRepository;
import com.laaguidi.SignFlow.repository.central.TenantRepository;
import com.laaguidi.SignFlow.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminAuthService {

    private final SuperAdminRepository superAdminRepository;
    private final CentralRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SuperAdminMapper superAdminMapper;
    private final CentralMonitoringService monitoringService;
    private final HttpServletRequest httpRequest;
    private final TenantRepository tenantRepository;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${application.security.jwt.cookie-domain}")
    private String cookieDomain;

    /**
     * Authenticates a Central Super Admin and sets HttpOnly JWT Cookies.
     */
    @Transactional
    public SuperAdminResponse login(AuthRequest request, HttpServletResponse response) {
        // 1. Fetch SuperAdmin from Central DB
        SuperAdmin admin = superAdminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // 2. Verify Password manually using PasswordEncoder
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // 3. Generate Access Token (Central Scope)
        String accessToken = jwtService.generateToken(admin, "public");

        // 4. Generate & Persist Refresh Token
        revokeAllUserRefreshTokens(admin);
        CentralRefreshToken refreshToken = createRefreshToken(admin);

        // 5. Set HttpOnly Cookies
        setCookie(response, "central_access_token", accessToken, jwtExpiration / 1000, "/api/v1/central");
        setCookie(response, "central_refresh_token", refreshToken.getToken(), refreshExpiration / 1000, "/api/v1/central/auth/refresh");

        log.info("SuperAdmin logged in successfully: {}", admin.getEmail());

        // 6. Audit Logging
        monitoringService.logEvent(
                "SUPER_ADMIN_LOGIN",
                admin.getEmail(),
                "super_admin:" + admin.getId(),
                "Successful authentication to Central Portal",
                getClientIpAddress(httpRequest)
        );

        return superAdminMapper.superAdminToSuperAdminResponse(admin);
    }
    /**
     * Registers a new Central SuperAdmin (Invocable only by existing SuperAdmins).
     */
    @Transactional
    public SuperAdminResponse registerSuperAdmin(SuperAdminRegisterRequest request) {
        if (superAdminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("SuperAdmin with email " + request.getEmail() + " already exists!");
        }

        SuperAdmin superAdmin = SuperAdmin.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("SUPER_ADMIN")
                .build();

        SuperAdmin savedAdmin = superAdminRepository.save(superAdmin);
        log.info("New SuperAdmin registered by existing squad member: {}", savedAdmin.getEmail());

        // Audit Logging
        monitoringService.logEvent(
                "SUPER_ADMIN_REGISTERED",
                getAuthenticatedAdminEmail(),
                "super_admin:" + savedAdmin.getId(),
                "Registered new Super Admin: " + savedAdmin.getEmail(),
                getClientIpAddress(httpRequest)
        );

        return superAdminMapper.superAdminToSuperAdminResponse(savedAdmin);
    }

    /**
     * Refreshes the SuperAdmin Access Token using a valid Central Refresh Token.
     */
    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String tokenStr = extractCookieValue(request);

        if (tokenStr == null) {
            throw new UnauthorizedException("Central Refresh token missing");
        }

        CentralRefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        SuperAdmin admin = refreshToken.getSuperAdmin();

        // تمرير admin المباشر بدلاً من خلق أوبجيكت User يدوي
        String newAccessToken = jwtService.generateToken(admin, "public");

        setCookie(response, "central_access_token", newAccessToken, jwtExpiration / 1000, "/api/v1/central");

        // Audit Logging
        monitoringService.logEvent(
                "SUPER_ADMIN_TOKEN_REFRESH",
                admin.getEmail(),
                "super_admin:" + admin.getId(),
                "Access token successfully refreshed",
                getClientIpAddress(request)
        );
    }

    /**
     * Generates a temporary impersonation token for a SuperAdmin to access a specific Tenant.
     */
    @Transactional(readOnly = true)
    public void impersonateTenant(UUID tenantId, HttpServletResponse response) throws BadRequestException {
        // 1. التأكد من أن SuperAdmin موثق فعلاً
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Must be authenticated as SuperAdmin");
        }

        // 2. التأكد من وجود الـ Tenant فـ Central DB
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (tenant.getStatus()!= Status.ACTIVE) {
            throw new BadRequestException("Cannot impersonate an inactive tenant");
        }

        SuperAdmin admin = superAdminRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("SuperAdmin profile not found"));

        // 3. توليد claims إضافية للتوكين المؤقت
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("tenantId", tenantId);
        extraClaims.put("isImpersonated", true);
        extraClaims.put("originalUser", admin.getEmail());

        // 4. توليد Access Token بمدة صلاحية قصيرة (مثلاً 30 دقيقة)
        String impersonationToken = jwtService.generateToken(extraClaims, admin);

        // 5. وضع التوكين فـ HttpOnly Cookie خاصة بالـ Tenant
        setCookie(response, "access_token", impersonationToken, 30 * 60, "/");

        // 6. Audit Logging للعملية أمنياً
        monitoringService.logEvent(
                "SUPER_ADMIN_IMPERSONATE",
                admin.getEmail(),
                "tenant:" + tenantId,
                "SuperAdmin started impersonating tenant: " + tenantId,
                getClientIpAddress(httpRequest)
        );
    }

    public void exitImpersonation(HttpServletResponse response) {
        setCookie(response, "access_token", "", 0, "/");
    }


    /**
     * Creates and saves a new JWT Refresh Token for SuperAdmin.
     */
    private CentralRefreshToken createRefreshToken(SuperAdmin admin) {
        String refreshTokenJwt = jwtService.generateRefreshToken(admin, "public");

        CentralRefreshToken refreshToken = CentralRefreshToken.builder()
                .superAdmin(admin)
                .token(refreshTokenJwt)
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }


    /**
     * Revokes all previous valid refresh tokens for a specific SuperAdmin.
     */
    private void revokeAllUserRefreshTokens(SuperAdmin admin) {
        var validTokens = refreshTokenRepository.findAllValidTokensBySuperAdmin(admin.getId());
        if (!validTokens.isEmpty()) {
            validTokens.forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(validTokens);
        }
    }

    /**
     * Securely sets HttpOnly SameSite Cookies.
     */
    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // رجعها true فـ Production مع HTTPS
                .path(path)
                .domain(cookieDomain)
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("central_refresh_token".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    // ==================== AUDIT HELPER METHODS ====================

    private String getAuthenticatedAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }


    public SuperAdminResponse getUser() {
        String email = getAuthenticatedAdminEmail();

        SuperAdmin superAdmin = superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("SuperAdmin not found with email: " + email));

        return superAdminMapper.superAdminToSuperAdminResponse(superAdmin);
    }
}