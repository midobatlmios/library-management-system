package com.library.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionException extends LibraryException {
    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, HttpStatus status) {
        super(message, status);
    }
}
