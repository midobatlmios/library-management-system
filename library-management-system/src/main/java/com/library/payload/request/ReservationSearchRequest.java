package com.library.payload.request;

import lombok.Data;

import java.time.LocalDate;

/** Every filter is optional. userId is ignored on /my/search (always the logged-in user). */
@Data
public class ReservationSearchRequest {
    private Long userId;
    private Long bookId;
    /** PENDING, AVAILABLE, FULFILLED, EXPIRED or CANCELLED */
    private String status;
    /** true = only PENDING and AVAILABLE */
    private Boolean activeOnly;
    private LocalDate reservedFrom;
    private LocalDate reservedTo;
    private int page = 0;
    private int size = 20;
    private String sortBy = "reservedAt";
    private String sortDirection = "DESC";
}
