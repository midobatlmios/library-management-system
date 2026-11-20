package com.library.payload.response;

import com.library.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueuePositionResponse {
    private Long reservationId;
    private ReservationStatus status;
    /** 1 = next in line; 0 = a copy is waiting for you; null = reservation closed. */
    private Integer queuePosition;
    private LocalDateTime availableUntil;
    private String message;
}
