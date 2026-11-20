package com.library.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Admin corrections. Every field is optional; only the ones sent are changed.
 * Status is not editable here: use check-in to return a book, so copy counts stay correct.
 */
@Data
public class UpdateLoanRequest {
    private LocalDate dueDate;

    @Min(value = 0, message = "Max renewals must be 0 or more")
    @Max(value = 10, message = "Max renewals must be at most 10")
    private Integer maxRenewals;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
