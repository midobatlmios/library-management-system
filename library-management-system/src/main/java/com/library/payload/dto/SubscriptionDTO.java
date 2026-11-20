package com.library.payload.dto;

import com.library.domain.SubscriptionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only view of a subscription. */
@Data
public class SubscriptionDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private Long planId;
    private String planName;
    private String planCode;
    /** Minor units, e.g. 19900 */
    private Long price;
    /** Major units, e.g. 199.00 */
    private BigDecimal priceInMajorUnits;
    private String currency;
    private SubscriptionStatus status;
    private boolean valid;
    private long daysRemaining;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private Integer maxBooksAllowed;
    private Integer maxBorrowDays;
    private Boolean autoRenew;
    private Long renewedFromId;
    private LocalDateTime activatedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
