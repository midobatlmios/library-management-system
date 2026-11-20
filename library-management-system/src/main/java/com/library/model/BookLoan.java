package com.library.model;

import com.library.domain.BookLoanStatus;
import com.library.domain.BookLoanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "book_loans", indexes = {
        @Index(name = "idx_loan_user_id", columnList = "user_id"),
        @Index(name = "idx_loan_book_id", columnList = "book_id"),
        @Index(name = "idx_loan_status", columnList = "status"),
        @Index(name = "idx_loan_due_date", columnList = "dueDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookLoan {

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
    private BookLoanType type = BookLoanType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookLoanStatus status = BookLoanStatus.CHECKED_OUT;

    @Column(nullable = false)
    private LocalDate checkoutDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Builder.Default
    private Integer renewalCount = 0;

    @Builder.Default
    private Integer maxRenewals = 2;

    @Column(length = 500)
    private String notes;

    /** Saved value, refreshed by the overdue job and on return. Use isLate()/getCurrentOverdueDays() for live values. */
    @Builder.Default
    private Boolean isOverdue = false;

    @Builder.Default
    private Integer overdueDays = 0;

    /** The due date a "due soon" reminder was already emailed for (a renewal moves the due date, so a new reminder is sent). */
    private LocalDate dueReminderSentFor;

    /** Fines attached to this loan (overdue, damage, lost...). They stay linked after the book is returned. */
    @OneToMany(mappedBy = "bookLoan", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Fine> fines = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---- Business logic ----

    /** Not yet returned. */
    public boolean isActive() {
        return status == BookLoanStatus.CHECKED_OUT || status == BookLoanStatus.OVERDUE;
    }

    /** Late days as of today (or as of the return date once returned). */
    public int getCurrentOverdueDays() {
        LocalDate until = returnDate != null ? returnDate : LocalDate.now();
        long days = ChronoUnit.DAYS.between(dueDate, until);
        return (int) Math.max(0, days);
    }

    public boolean isLate() {
        return getCurrentOverdueDays() > 0;
    }

    public boolean canBeRenewed() {
        return status == BookLoanStatus.CHECKED_OUT && !isLate() && renewalCount < maxRenewals;
    }

    /** Stores the live overdue values and moves CHECKED_OUT -> OVERDUE when late. */
    public void refreshOverdue() {
        this.overdueDays = getCurrentOverdueDays();
        this.isOverdue = overdueDays > 0;
        if (isOverdue && status == BookLoanStatus.CHECKED_OUT) {
            this.status = BookLoanStatus.OVERDUE;
        }
    }
}
