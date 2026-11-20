package com.library.domain;

/**
 * PENDING   -> created, waiting for payment
 * ACTIVE    -> paid; gives borrow rights between startDate and endDate
 * EXPIRED   -> endDate has passed
 * CANCELLED -> cancelled by the user/admin, or an unpaid PENDING one was replaced
 */
public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
