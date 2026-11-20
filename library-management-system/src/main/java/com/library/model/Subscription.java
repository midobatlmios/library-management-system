package com.library.model;

import com.library.domain.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_sub_user_id", columnList = "user_id"),
        @Index(name = "idx_sub_status", columnList = "status"),
        @Index(name = "idx_sub_end_date", columnList = "endDate"),
        @Index(name = "idx_sub_plan_id", columnList = "plan_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    // ---- Copied from the plan when subscribing, so history stays correct if the plan changes later ----
    @Column(nullable = false, length = 150)
    private String planName;

    @Column(nullable = false, length = 50)
    private String planCode;

    /** In minor units (centimes): 19900 = 199.00 MAD. */
    @Column(nullable = false)
    private Long price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer maxBooksAllowed;

    @Column(nullable = false)
    private Integer maxBorrowDays;

    /** Plan duration + trial days (trial only on a user's first subscription). Used to compute endDate. */
    @Column(nullable = false)
    private Integer durationDays;

    // ---- Lifecycle ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    /** Set on activation. */
    private LocalDate startDate;

    /** Last day the subscription is valid (inclusive). Set on activation. */
    private LocalDate endDate;

    @Builder.Default
    private Boolean autoRenew = false;

    /** Reference of the payment that must be confirmed to activate this subscription. */
    @Column(length = 100)
    private String paymentId;

    /** Set when this subscription was created by renewing another one. */
    private Long renewedFromId;

    private LocalDateTime activatedAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---- Business logic (from the documentation) ----

    /** Paid and today is inside [startDate, endDate]. */
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return status == SubscriptionStatus.ACTIVE
                && startDate != null && endDate != null
                && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    /** Days left including today; 0 when not valid. */
    public long getDaysRemaining() {
        if (!isValid()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate) + 1;
    }

    /** endDate is the last valid day, so a 30-day plan starting on the 1st ends on the 30th. */
    public void calculateEndDate() {
        this.endDate = startDate.plusDays(durationDays - 1L);
    }

    public void initializeFromPlan(SubscriptionPlan plan, int extraTrialDays) {
        this.plan = plan;
        this.planName = plan.getTitle();
        this.planCode = plan.getPlanCode();
        this.price = plan.getPrice().movePointRight(2).longValueExact();
        this.currency = plan.getCurrency();
        this.maxBooksAllowed = plan.getMaxBooksAllowed();
        this.maxBorrowDays = plan.getMaxBorrowDays();
        this.durationDays = plan.getDurationInDays() + extraTrialDays;
    }

    /** 19900 -> 199.00 */
    public BigDecimal getPriceInMajorUnits() {
        return BigDecimal.valueOf(price, 2);
    }

    /** Used by Book Loan (step 6): can the user borrow one more book? */
    public boolean canBorrowBooks(int currentlyBorrowed) {
        return isValid() && currentlyBorrowed < maxBooksAllowed;
    }
}
