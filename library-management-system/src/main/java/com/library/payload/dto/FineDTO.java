package com.library.payload.dto;

import com.library.domain.FineStatus;
import com.library.domain.FineType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only view of a fine. Amounts in minor units, plus the same values in MAD for display. */
@Data
public class FineDTO {
    private Long id;
    private Long bookLoanId;
    private Long bookId;
    private String bookTitle;
    private Long userId;
    private String userEmail;
    private FineType type;
    private FineStatus status;
    private Long amount;
    private Long amountPaid;
    private Long amountOutstanding;
    private BigDecimal amountInMajorUnits;
    private BigDecimal amountOutstandingInMajorUnits;
    private String currency;
    private String reason;
    private String notes;
    private Long waivedById;
    private LocalDateTime waivedAt;
    private String waiverReason;
    private LocalDateTime paidAt;
    private Long processedById;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
