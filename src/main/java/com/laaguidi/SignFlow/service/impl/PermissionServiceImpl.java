package com.laaguidi.SignFlow.service.impl;

import com.laaguidi.SignFlow.model.dto.response.PermissionResponse;
import com.laaguidi.SignFlow.model.mapper.PermissionMapper;
import com.laaguidi.SignFlow.repository.PermissionRepository;
import com.laaguidi.SignFlow.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}