package com.library.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenewLoanRequest {
    @NotNull(message = "Book loan id is required")
    private Long bookLoanId;

    /** Optional. Defaults to the subscription's maxBorrowDays and cannot exceed it. */
    @Min(value = 1, message = "Extend days must be at least 1")
    private Integer extendDays;
}
