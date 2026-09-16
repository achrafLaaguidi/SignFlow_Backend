package com.laaguidi.SignFlow.controller.central;

import com.laaguidi.SignFlow.model.central.dto.request.SuperAdminRegisterRequest;
import com.laaguidi.SignFlow.model.central.dto.response.SuperAdminResponse;
import com.laaguidi.SignFlow.service.central.SuperAdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/central/admins")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class CentralAdminController {

    private final SuperAdminAuthService superAdminAuthService;

    @PostMapping("/register")
    public ResponseEntity<SuperAdminResponse> registerSuperAdmin(
            @Valid @RequestBody SuperAdminRegisterRequest request
    ) {
        SuperAdminResponse response = superAdminAuthService.registerSuperAdmin(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}