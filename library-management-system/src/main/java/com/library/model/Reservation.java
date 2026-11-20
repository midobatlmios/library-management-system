package com.library.model;

import com.library.domain.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A hold on a book that has no copies on the shelf. Users wait in a queue per book;
 * when a copy comes back it is held for the first user in line.
 */
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_res_user_id", columnList = "user_id"),
        @Index(name = "idx_res_book_id", columnList = "book_id"),
        @Index(name = "idx_res_status", columnList = "status"),
        @Index(name = "idx_res_reserved_at", columnList = "reservedAt"),
        @Index(name = "idx_res_available_until", columnList = "availableUntil")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    /** When the reservation was made; the queue is ordered by this. */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    /** When a copy was set aside for this user. */
    private LocalDateTime availableAt;

    /** Pickup deadline. */
    private LocalDateTime availableUntil;

    private LocalDateTime fulfilledAt;

    /** When it was cancelled or expired. */
    private LocalDateTime cancelledAt;

    /** 1 = next in line. 0 = a copy is waiting for you. Null once the reservation is closed. */
    private Integer queuePosition;

    /** Whether the "your book is ready" notice was sent (email arrives in step 12). */
    @Builder.Default
    private Boolean notificationSent = false;

    /** The loan created when the reservation was fulfilled. */
    private Long fulfilledLoanId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---- Business logic (from the documentation) ----

    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.AVAILABLE;
    }

    public boolean canBeCancelled() {
        return isActive();
    }

    /** A held copy whose pickup deadline has passed. */
    public boolean hasExpired() {
        return status == ReservationStatus.AVAILABLE
                && availableUntil != null
                && availableUntil.isBefore(LocalDateTime.now());
    }
}
