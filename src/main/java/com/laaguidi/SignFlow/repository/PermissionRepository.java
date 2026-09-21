package com.laaguidi.SignFlow.repository;

import com.laaguidi.SignFlow.model.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
