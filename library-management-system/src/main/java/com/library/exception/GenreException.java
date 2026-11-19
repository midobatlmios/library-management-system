package com.library.exception;

import org.springframework.http.HttpStatus;

public class GenreException extends LibraryException {
    public GenreException(String message) {
        super(message);
    }

    public GenreException(String message, HttpStatus status) {
        super(message, status);
    }
}
