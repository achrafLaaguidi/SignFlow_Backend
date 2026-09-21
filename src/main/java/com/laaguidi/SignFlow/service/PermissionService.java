package com.laaguidi.SignFlow.service;

import com.laaguidi.SignFlow.model.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getAllPermissions();
}