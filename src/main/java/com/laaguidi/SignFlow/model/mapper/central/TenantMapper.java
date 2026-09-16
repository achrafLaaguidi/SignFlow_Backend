package com.laaguidi.SignFlow.model.mapper.central;

import com.laaguidi.SignFlow.model.central.entity.Tenant;
import com.laaguidi.SignFlow.model.dto.response.TenantResponse;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;


@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantResponse tenantToTenantResponse(Tenant tenant);
    List<TenantResponse> tenantToTenantResponse(List<Tenant> tenants);
}
