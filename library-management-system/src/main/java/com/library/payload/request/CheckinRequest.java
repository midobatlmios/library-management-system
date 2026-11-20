package com.library.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckinRequest {
    @NotNull(message = "Book loan id is required")
    private Long bookLoanId;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
