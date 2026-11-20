package com.library.payload.request;

import com.library.domain.BookLoanType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull(message = "Book id is required")
    private Long bookId;

    /** Optional. Defaults to the subscription's maxBorrowDays and cannot exceed it. */
    @Min(value = 1, message = "Due days must be at least 1")
    private Integer dueDays;

    /** Optional. Defaults to SHORT_TERM for 7 days or less, NORMAL otherwise. */
    private BookLoanType type;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
