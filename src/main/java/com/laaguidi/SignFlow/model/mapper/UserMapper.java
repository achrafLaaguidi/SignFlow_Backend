package com.laaguidi.SignFlow.model.mapper;


import com.laaguidi.SignFlow.model.dto.request.RegisterRequest;
import com.laaguidi.SignFlow.model.dto.response.UserResponse;
import com.laaguidi.SignFlow.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(RegisterRequest request);
}