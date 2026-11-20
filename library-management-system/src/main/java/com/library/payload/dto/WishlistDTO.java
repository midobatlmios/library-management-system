package com.library.payload.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** A saved book, with enough book details for a wishlist page ("available now", cover, etc.). */
@Data
public class WishlistDTO {
    private Long id;
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private String coverImageUrl;
    private Integer availableCopies;
    /** False if the library removed the book from the catalog after it was saved. */
    private Boolean bookActive;
    private String notes;
    private LocalDateTime addedAt;
}
