package com.library.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** One review per user per book (a deleted review is reused if the user reviews the book again). */
@Entity
@Table(name = "book_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_book_review", columnNames = {"user_id", "book_id"}),
        indexes = {
                @Index(name = "idx_review_book_id", columnList = "book_id"),
                @Index(name = "idx_review_user_id", columnList = "user_id"),
                @Index(name = "idx_review_rating", columnList = "rating")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    /** 1 to 5 stars. */
    @Column(nullable = false)
    private Integer rating;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String reviewText;

    /** True when the reviewer has returned a loan of this book. */
    @Builder.Default
    private Boolean isVerifiedRead = false;

    /** Soft delete flag. */
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer helpfulCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
