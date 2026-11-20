package com.library.exception;

import org.springframework.http.HttpStatus;

public class WishlistException extends LibraryException {
    public WishlistException(String message) {
        super(message);
    }

    public WishlistException(String message, HttpStatus status) {
        super(message, status);
    }
}
