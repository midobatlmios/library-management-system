package com.library.exception;

import org.springframework.http.HttpStatus;

public class UserException extends LibraryException {
    public UserException(String message) {
        super(message);
    }

    public UserException(String message, HttpStatus status) {
        super(message, status);
    }
}
