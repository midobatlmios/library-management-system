package com.library.domain;

public enum FineType {
    /** Book returned after its due date (created automatically on return). */
    OVERDUE,
    /** Book returned damaged. */
    DAMAGE,
    /** Book lost by the borrower. */
    LOST,
    /** Any other fee (e.g. card replacement). */
    OTHER
}
