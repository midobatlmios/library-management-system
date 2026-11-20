package com.library.exception;

import org.springframework.http.HttpStatus;

public class BookReviewException extends LibraryException {
    public BookReviewException(String message) {
        super(message);
    }

    public BookReviewException(String message, HttpStatus status) {
        super(message, status);
    }
}
