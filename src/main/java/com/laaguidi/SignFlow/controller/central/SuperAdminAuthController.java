package com.laaguidi.SignFlow.controller.central;

import com.laaguidi.SignFlow.model.dto.request.AuthRequest;
import com.laaguidi.SignFlow.model.central.dto.response.SuperAdminResponse;
import com.laaguidi.SignFlow.service.central.SuperAdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/central/auth")
@RequiredArgsConstructor
public class SuperAdminAuthController {

    private final SuperAdminAuthService superAdminAuthService;

    /**
     * Authenticates SuperAdmin and returns HTTP-Only session cookies.
     */
    @PostMapping("/login")
    public ResponseEntity<SuperAdminResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(superAdminAuthService.login(request, response));
    }

    @GetMapping("/me")
    public ResponseEntity<SuperAdminResponse> getUser(){
        return ResponseEntity.ok(superAdminAuthService.getUser());
    }

    /**
     * Refreshes Access Token for active SuperAdmin session.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        superAdminAuthService.refreshToken(request, response);
        return ResponseEntity.ok().build();
    }
}