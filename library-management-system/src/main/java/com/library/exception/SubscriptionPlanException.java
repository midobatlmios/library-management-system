package com.library.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionPlanException extends LibraryException {
    public SubscriptionPlanException(String message) {
        super(message);
    }

    public SubscriptionPlanException(String message, HttpStatus status) {
        super(message, status);
    }
}
