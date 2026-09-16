package com.laaguidi.SignFlow.model.mapper;

import com.laaguidi.SignFlow.model.dto.response.PermissionResponse;
import com.laaguidi.SignFlow.model.entity.Permission;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    Set<PermissionResponse> toResponseSet(Set<Permission> permissions);
}
