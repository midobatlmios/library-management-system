package com.library.exception;

import org.springframework.http.HttpStatus;

public class FineException extends LibraryException {
    public FineException(String message) {
        super(message);
    }

    public FineException(String message, HttpStatus status) {
        super(message, status);
    }
}
