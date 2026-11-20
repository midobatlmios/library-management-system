package com.library.repository;

import com.library.domain.BookLoanStatus;
import com.library.domain.BookLoanType;
import com.library.model.BookLoan;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Small reusable filters for the admin loan search. Each returns null when the filter is not set,
 * and Spring ignores null specifications when they are combined.
 */
public final class BookLoanSpecifications {

    private BookLoanSpecifications() {
    }

    public static Specification<BookLoan> userId(Long userId) {
        return userId == null ? null : (root, q, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<BookLoan> bookId(Long bookId) {
        return bookId == null ? null : (root, q, cb) -> cb.equal(root.get("book").get("id"), bookId);
    }

    public static Specification<BookLoan> statusIn(Collection<BookLoanStatus> statuses) {
        return statuses == null ? null : (root, q, cb) -> root.get("status").in(statuses);
    }

    public static Specification<BookLoan> type(BookLoanType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    /** Late right now: not returned and due date passed, whether or not the overdue job has run. */
    public static Specification<BookLoan> lateNow(Boolean overdueOnly) {
        if (!Boolean.TRUE.equals(overdueOnly)) {
            return null;
        }
        return (root, q, cb) -> cb.and(
                root.get("status").in(BookLoanStatus.CHECKED_OUT, BookLoanStatus.OVERDUE),
                cb.lessThan(root.get("dueDate"), LocalDate.now()));
    }

    public static Specification<BookLoan> between(String field, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get(field), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }
}
