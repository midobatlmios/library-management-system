package com.library.payload.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Public view of a review. The reviewer's email is never included. */
@Data
public class BookReviewDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long userId;
    private String userFullName;
    private Integer rating;
    private String title;
    private String reviewText;
    private Boolean isVerifiedRead;
    private Boolean isActive;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
