package com.laaguidi.SignFlow.service;

import com.laaguidi.SignFlow.model.dto.request.RoleRequest;
import com.laaguidi.SignFlow.model.dto.response.RoleNameResponse;
import com.laaguidi.SignFlow.model.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    List<RoleNameResponse> getAllRolesNames();
    RoleResponse getRoleById(UUID id);
    RoleResponse createRole(RoleRequest request);
    RoleResponse updateRole(UUID id, RoleRequest request);
    void deleteRole(UUID id);
}