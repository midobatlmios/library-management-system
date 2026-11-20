package com.library.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReservationRequest {
    @NotNull(message = "Book id is required")
    private Long bookId;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
