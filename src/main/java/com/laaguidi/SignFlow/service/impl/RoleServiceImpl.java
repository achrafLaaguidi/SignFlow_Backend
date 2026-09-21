package com.laaguidi.SignFlow.service.impl;

import com.laaguidi.SignFlow.exception.ConflictException;
import com.laaguidi.SignFlow.exception.NotFoundException;
import com.laaguidi.SignFlow.model.dto.request.RoleRequest;
import com.laaguidi.SignFlow.model.dto.response.RoleNameResponse;
import com.laaguidi.SignFlow.model.dto.response.RoleResponse;
import com.laaguidi.SignFlow.model.entity.Permission;
import com.laaguidi.SignFlow.model.entity.Role;
import com.laaguidi.SignFlow.model.mapper.RoleMapper;
import com.laaguidi.SignFlow.repository.PermissionRepository;
import com.laaguidi.SignFlow.repository.RoleRepository;
import com.laaguidi.SignFlow.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleNameResponse> getAllRolesNames() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponseNames)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        request.setName("CLIENT_"+request.getName().toUpperCase());

        if (roleRepository.existsByName(request.getName())) {
            throw new ConflictException("Role with name " + request.getName() + " already exists");
        }

        List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        if (permissions.size() != request.getPermissionIds().size()) {
            throw new NotFoundException("One or more permission IDs are invalid");
        }
        Role role = roleMapper.toEntity(request);
        role.setPermissions(new HashSet<>(permissions));

        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));

        request.setName("CLIENT_"+request.getName().toUpperCase());

        if (roleRepository.existsByName(request.getName()) && !role.getName().equals(request.getName())) {
            throw new ConflictException("Role with name " + request.getName() + " already exists");
        }
        role.setName(request.getName());
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            if (permissions.size() != request.getPermissionIds().size()) {
                throw new NotFoundException("One or more permission IDs are invalid");
            }
            role.setPermissions(new HashSet<>(permissions));
        }

        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new NotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}