package com.laaguidi.SignFlow.model.mapper.central;

import com.laaguidi.SignFlow.model.central.dto.response.AuditLogResponse;
import com.laaguidi.SignFlow.model.central.entity.CentralAuditLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CentralAuditLogsMapper {

    List<AuditLogResponse> toAuditLogResponseList(List<CentralAuditLog> auditLogs);
}
