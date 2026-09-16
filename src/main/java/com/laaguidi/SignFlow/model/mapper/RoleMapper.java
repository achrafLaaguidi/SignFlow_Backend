package com.laaguidi.SignFlow.model.mapper;


import com.laaguidi.SignFlow.model.dto.response.RoleResponse;
import com.laaguidi.SignFlow.model.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}