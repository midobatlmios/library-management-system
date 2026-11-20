package com.library.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubscribeRequest {
    @NotNull(message = "Plan id is required")
    private Long planId;

    /** Defaults to the plan's autoRenew setting. */
    private Boolean autoRenew;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;
}
