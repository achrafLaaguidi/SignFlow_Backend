package com.laaguidi.SignFlow.service.impl;

import com.laaguidi.SignFlow.exception.TenantNotFoundException;
import com.laaguidi.SignFlow.model.dto.request.AuthRequest;
import com.laaguidi.SignFlow.model.dto.request.CreateUserRequest;
import com.laaguidi.SignFlow.model.dto.response.UserResponse;
import com.laaguidi.SignFlow.model.entity.Role;
import com.laaguidi.SignFlow.model.entity.User;
import com.laaguidi.SignFlow.model.mapper.UserMapper;
import com.laaguidi.SignFlow.repository.RoleRepository;
import com.laaguidi.SignFlow.repository.UserRepository;
import com.laaguidi.SignFlow.security.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${application.security.jwt.cookie-domain}")
    private String cookieDomain;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RoleRepository roleRepository,
            UserMapper userMapper,
            AuthenticationManager authenticationManager,
            @Qualifier("tenantUserDetailsService") UserDetailsService userDetailsService) { // 👈 زدنا @Qualifier هنا
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public UserResponse register(CreateUserRequest request, String tenantId) {
        validateTenantId(tenantId);

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found in current tenant scope"));

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Authenticates a user in the context of a specific Tenant.
     */
    public UserResponse login(AuthRequest request, String tenantId, HttpServletResponse response) {
        validateTenantId(tenantId);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user, "tenant_"+tenantId);
        String refreshToken = jwtService.generateRefreshToken(user, "tenant_"+tenantId);

        setCookie(response, "access_token", accessToken, (int) (jwtExpiration / 1000), "/");
        setCookie(response, "refresh_token", refreshToken, (int) (refreshExpiration / 1000), "/api/v1/auth/refresh");

        return userMapper.toResponse(user);
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refresh_token");

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token missing");
        }

        String userEmail = jwtService.extractUsername(refreshToken);
        String tenantId = jwtService.extractTenantId(refreshToken); // Extract tenant_id from existing refresh token

        if (userEmail != null && tenantId != null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                // Pass tenantId when regenerating new tokens
                String newAccessToken = jwtService.generateToken(userDetails, tenantId);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails, tenantId);

                setCookie(response, "access_token", newAccessToken, (int) (jwtExpiration / 1000), "/");
                setCookie(response, "refresh_token", newRefreshToken, (int) (refreshExpiration / 1000), "/auth/refresh");
            } else {
                throw new RuntimeException("Invalid refresh token");
            }
        } else {
            throw new RuntimeException("Invalid token claims");
        }
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new TenantNotFoundException("X-Tenant-ID header is required but missing or empty");
        }

    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(path);
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }


    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getAuthenticatedAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    public UserResponse getUser() {
        String email = getAuthenticatedAdminEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("SuperAdmin not found with email: " + email));

        return userMapper.toResponse(user);
    }
}