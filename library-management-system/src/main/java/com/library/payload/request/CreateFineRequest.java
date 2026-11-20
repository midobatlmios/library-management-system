package com.library.payload.request;

import com.library.domain.FineType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFineRequest {
    @NotNull(message = "Book loan id is required")
    private Long bookLoanId;

    /** Optional: the fine always goes to the loan's borrower. If sent, it must match. */
    private Long userId;

    @NotNull(message = "Fine type is required")
    private FineType type;

    /** Minor units (centimes): 2500 = 25.00 MAD. */
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Long amount;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;
}
