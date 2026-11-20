package com.library.domain;

/**
 * PENDING   -> waiting for the user to pay (declined attempts keep it PENDING, up to the attempt limit)
 * SUCCESS   -> paid; the subscription/fine has been updated
 * FAILED    -> too many declined attempts
 * EXPIRED   -> not paid in time
 * CANCELLED -> no longer needed (replaced by a newer payment, cancelled by the user, fine waived...)
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED
}
