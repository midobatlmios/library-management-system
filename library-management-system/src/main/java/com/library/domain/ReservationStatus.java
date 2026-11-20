package com.library.domain;

/**
 * PENDING   -> waiting in the queue for a copy
 * AVAILABLE -> a returned copy is held for this user until availableUntil
 * FULFILLED -> the user borrowed the held copy
 * EXPIRED   -> the user did not pick it up in time
 * CANCELLED -> cancelled by the user or an admin
 */
public enum ReservationStatus {
    PENDING,
    AVAILABLE,
    FULFILLED,
    EXPIRED,
    CANCELLED
}
