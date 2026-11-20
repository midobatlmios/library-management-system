package com.library.exception;

import org.springframework.http.HttpStatus;

public class ReservationException extends LibraryException {
    public ReservationException(String message) {
        super(message);
    }

    public ReservationException(String message, HttpStatus status) {
        super(message, status);
    }
}
