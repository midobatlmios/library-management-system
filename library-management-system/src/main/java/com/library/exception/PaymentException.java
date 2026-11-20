package com.library.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends LibraryException {
    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, HttpStatus status) {
        super(message, status);
    }
}
