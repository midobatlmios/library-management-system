package com.library.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaiveFineRequest {
    @NotNull(message = "Fine id is required")
    private Long fineId;

    @NotBlank(message = "Waiver reason is required")
    @Size(max = 500, message = "Waiver reason must be at most 500 characters")
    private String waiverReason;
}
