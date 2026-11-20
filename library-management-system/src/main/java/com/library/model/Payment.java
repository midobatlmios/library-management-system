package com.library.model;

import com.library.domain.PaymentReferenceType;
import com.library.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One request to pay something (a subscription or a fine). Card numbers are NEVER stored:
 * only the last 4 digits of the card used for a successful payment.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_reference", columnList = "referenceType, referenceId"),
        @Index(name = "idx_payment_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, unguessable reference used in URLs, e.g. PAY_3F9A... */
    @Column(nullable = false, unique = true, length = 40)
    private String paymentId;

    /** The person who owes the money. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentReferenceType referenceType;

    /** Id of the Subscription or Fine. */
    @Column(nullable = false)
    private Long referenceId;

    /** Shown on the checkout page, e.g. "Subscription: Gold Annual Plan". */
    @Column(nullable = false, length = 255)
    private String description;

    /** Minor units (centimes): 19900 = 199.00 MAD. */
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Which gateway handled it, e.g. SIMULATED. */
    @Column(nullable = false, length = 30)
    private String provider;

    /** The gateway's own transaction reference (set on success). */
    @Column(length = 100)
    private String providerTransactionId;

    @Builder.Default
    private Integer attempts = 0;

    /** Message from the last declined attempt or the reason it was cancelled. */
    @Column(length = 255)
    private String lastError;

    @Column(length = 4)
    private String cardLast4;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Optimistic locking: two simultaneous updates of the same payment can't both succeed. */
    @Version
    private Long version;

    public boolean isExpiredNow() {
        return status == PaymentStatus.PENDING && expiresAt.isBefore(LocalDateTime.now());
    }

    public BigDecimal getAmountInMajorUnits() {
        return BigDecimal.valueOf(amount, 2);
    }
}
