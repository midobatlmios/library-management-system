package com.library.model;

import com.library.domain.FineStatus;
import com.library.domain.FineType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A money penalty linked to a book loan. Amounts are in minor units (centimes): 2500 = 25.00 MAD.
 */
@Entity
@Table(name = "fines", indexes = {
        @Index(name = "idx_fine_user_id", columnList = "user_id"),
        @Index(name = "idx_fine_loan_id", columnList = "book_loan_id"),
        @Index(name = "idx_fine_status", columnList = "status"),
        @Index(name = "idx_fine_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_loan_id")
    private BookLoan bookLoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineType type;

    /** Total fine, minor units. */
    @Column(nullable = false)
    private Long amount;

    /** Paid so far, minor units. */
    @Column(nullable = false)
    @Builder.Default
    private Long amountPaid = 0L;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FineStatus status = FineStatus.PENDING;

    @Column(length = 500)
    private String reason;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by_id")
    private User waivedBy;

    private LocalDateTime waivedAt;

    @Column(length = 500)
    private String waiverReason;

    private LocalDateTime paidAt;

    /** Staff member who recorded a desk payment (null for online payments). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    /** Reference of the last payment applied. */
    @Column(length = 100)
    private String transactionId;

    /** Payment started online and waiting for confirmation (simulated gateway, step 11). */
    @Column(length = 100)
    private String pendingPaymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---- Business logic (from the documentation) ----

    public long getAmountOutstanding() {
        if (status == FineStatus.WAIVED) {
            return 0;
        }
        return Math.max(0, amount - amountPaid);
    }

    public boolean isFullyPaid() {
        return status == FineStatus.PAID || amountPaid >= amount;
    }

    public boolean isWaived() {
        return status == FineStatus.WAIVED;
    }

    /** Still owes money. */
    public boolean isPending() {
        return status == FineStatus.PENDING || status == FineStatus.PARTIALLY_PAID;
    }

    /** Applies a payment (minor units). Caller checks 0 < payment <= outstanding. */
    public void applyPayment(long payment, String transactionId, User processedBy) {
        this.amountPaid += payment;
        this.transactionId = transactionId;
        this.processedBy = processedBy;
        this.pendingPaymentId = null;
        if (amountPaid >= amount) {
            this.status = FineStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else {
            this.status = FineStatus.PARTIALLY_PAID;
        }
    }

    public void waive(User admin, String reason) {
        this.status = FineStatus.WAIVED;
        this.waivedBy = admin;
        this.waivedAt = LocalDateTime.now();
        this.waiverReason = reason;
        this.pendingPaymentId = null;
    }

    public static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }
}
