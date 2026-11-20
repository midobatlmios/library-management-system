package com.library.payload.request;

import com.library.domain.BookLoanType;
import lombok.Data;

import java.time.LocalDate;

/** Admin search. Every filter is optional. */
@Data
public class LoanSearchRequest {
    private Long userId;
    private Long bookId;
    /** CHECKED_OUT, OVERDUE, RETURNED, or ACTIVE (= not returned yet). */
    private String status;
    private BookLoanType type;
    private Boolean overdueOnly;
    private LocalDate checkoutFrom;
    private LocalDate checkoutTo;
    private LocalDate dueFrom;
    private LocalDate dueTo;
    private int page = 0;
    private int size = 20;
    private String sortBy = "checkoutDate";
    private String sortDirection = "DESC";
}
