package com.laaguidi.SignFlow.model.mapper;


import com.laaguidi.SignFlow.model.dto.request.RoleRequest;
import com.laaguidi.SignFlow.model.dto.response.RoleNameResponse;
import com.laaguidi.SignFlow.model.dto.response.RoleResponse;
import com.laaguidi.SignFlow.model.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions")
    RoleResponse toResponse(Role role);

    RoleNameResponse toResponseNames(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleRequest request);
}