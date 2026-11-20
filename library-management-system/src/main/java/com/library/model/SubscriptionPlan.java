package com.library.model;

import com.library.domain.BillingCycle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscription_plan", indexes = {
        @Index(name = "idx_plan_active", columnList = "active"),
        @Index(name = "idx_plan_billing_cycle", columnList = "billingCycle")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short system code, e.g. PREMIUM_MONTHLY. Copied onto each Subscription (step 5) for invoices. */
    @Column(nullable = false, unique = true, length = 50)
    private String planCode;

    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** ISO currency code, e.g. INR, USD, MAD. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer durationInDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Builder.Default
    private Integer trialDays = 0;

    /** How many books a subscriber may borrow at the same time. */
    @Builder.Default
    private Integer maxBooksAllowed = 1;

    /** How many days each loan lasts for a subscriber. */
    @Builder.Default
    private Integer maxBorrowDays = 14;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature", length = 200)
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Builder.Default
    private Boolean autoRenew = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
