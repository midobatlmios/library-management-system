package com.library.exception;

import org.springframework.http.HttpStatus;

public class BookException extends LibraryException {
    public BookException(String message) {
        super(message);
    }

    public BookException(String message, HttpStatus status) {
        super(message, status);
    }
}
