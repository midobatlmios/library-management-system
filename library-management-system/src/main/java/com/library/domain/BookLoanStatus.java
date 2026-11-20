package com.library.domain;

/**
 * CHECKED_OUT -> the user has the book, due date not passed
 * OVERDUE     -> the user still has the book and the due date has passed
 * RETURNED    -> the book is back (returnDate is set)
 */
public enum BookLoanStatus {
    CHECKED_OUT,
    OVERDUE,
    RETURNED
}
