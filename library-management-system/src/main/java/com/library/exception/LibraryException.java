package com.library.exception;

import org.springframework.http.HttpStatus;

/**
 * Parent of every module exception (BookException, GenreException, UserException, ...).
 * Each one carries the HTTP status the API should answer with; 400 Bad Request by default.
 *
 * Usage:
 *   throw new BookException("Book not found with id: " + id, HttpStatus.NOT_FOUND);
 */
public abstract class LibraryException extends Exception {

    private final HttpStatus status;

    protected LibraryException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    protected LibraryException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
