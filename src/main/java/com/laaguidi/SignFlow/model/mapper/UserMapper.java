package com.laaguidi.SignFlow.model.mapper;


import com.laaguidi.SignFlow.model.dto.request.CreateUserRequest;
import com.laaguidi.SignFlow.model.dto.request.UpdateUserRequest;
import com.laaguidi.SignFlow.model.dto.response.UserResponse;
import com.laaguidi.SignFlow.model.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "email", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);
}