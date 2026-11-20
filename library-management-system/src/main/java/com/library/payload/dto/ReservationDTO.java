package com.library.payload.dto;

import com.library.domain.ReservationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** Read-only view of a reservation. */
@Data
public class ReservationDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private ReservationStatus status;
    private boolean active;
    private Integer queuePosition;
    private LocalDateTime reservedAt;
    private LocalDateTime availableAt;
    private LocalDateTime availableUntil;
    private LocalDateTime fulfilledAt;
    private LocalDateTime cancelledAt;
    private Boolean notificationSent;
    private Long fulfilledLoanId;
    private String notes;
    private LocalDateTime updatedAt;
}
