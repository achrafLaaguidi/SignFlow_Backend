package com.laaguidi.SignFlow.controller;


import com.laaguidi.SignFlow.config.tenant.TenantContext;
import com.laaguidi.SignFlow.model.dto.request.AuthRequest;
import com.laaguidi.SignFlow.model.dto.request.RegisterRequest;
import com.laaguidi.SignFlow.model.dto.response.UserResponse;
import com.laaguidi.SignFlow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        return ResponseEntity.ok(authService.register(request, tenantId));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody AuthRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, tenantId, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.refreshToken(request, response);
        return ResponseEntity.ok().build();
    }

}
