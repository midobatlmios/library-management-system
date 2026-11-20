package com.library.exception;

import org.springframework.http.HttpStatus;

public class BookLoanException extends LibraryException {
    public BookLoanException(String message) {
        super(message);
    }

    public BookLoanException(String message, HttpStatus status) {
        super(message, status);
    }
}
