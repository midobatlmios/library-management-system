package com.library.payload.dto;

import com.library.domain.BookLoanStatus;
import com.library.domain.BookLoanType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only view of a loan. */
@Data
public class BookLoanDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private BookLoanType type;
    private BookLoanStatus status;
    private LocalDate checkoutDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Integer renewalCount;
    private Integer maxRenewals;
    private Integer renewalsLeft;
    /** Live values, computed from today's date. */
    private Boolean isOverdue;
    private Integer overdueDays;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
