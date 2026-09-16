package com.laaguidi.SignFlow.model.mapper.central;

import com.laaguidi.SignFlow.model.central.dto.response.SuperAdminResponse;
import com.laaguidi.SignFlow.model.central.entity.SuperAdmin;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SuperAdminMapper {
    SuperAdminResponse superAdminToSuperAdminResponse(SuperAdmin superAdmin);

}
