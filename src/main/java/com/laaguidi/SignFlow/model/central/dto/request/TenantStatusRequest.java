package com.laaguidi.SignFlow.model.central.dto.request;

import com.laaguidi.SignFlow.model.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantStatusRequest {

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private Status status;
}