package com.library.repository;

import com.library.model.Book;
import com.library.model.BookReview;
import com.library.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    Optional<BookReview> findByUserAndBook(User user, Book book);

    boolean existsByUserAndBookAndIsActiveTrue(User user, Book book);

    Page<BookReview> findByBookAndIsActiveTrue(Book book, Pageable pageable);

    Page<BookReview> findByBookAndIsActiveTrueAndRating(Book book, Integer rating, Pageable pageable);

    Page<BookReview> findByBookAndIsActiveTrueAndIsVerifiedReadTrue(Book book, Pageable pageable);

    Page<BookReview> findByUserAndIsActiveTrue(User user, Pageable pageable);

    /** Adds one helpful vote in a single SQL statement (safe when many users vote at once). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookReview r set r.helpfulCount = r.helpfulCount + 1 where r.id = :id")
    int incrementHelpful(@Param("id") Long id);

    // ---- statistics for one book ----
    long countByBookAndIsActiveTrue(Book book);

    long countByBookAndIsActiveTrueAndIsVerifiedReadTrue(Book book);

    @Query("select avg(r.rating) from BookReview r where r.book = :book and r.isActive = true")
    Double averageRating(@Param("book") Book book);

    /** Rows of [rating, count]. */
    @Query("select r.rating, count(r) from BookReview r where r.book = :book and r.isActive = true group by r.rating")
    List<Object[]> ratingDistribution(@Param("book") Book book);

    // ---- statistics for admins ----
    long countByIsActiveTrue();

    long countByIsActiveFalse();

    long countByIsActiveTrueAndIsVerifiedReadTrue();

    long countByIsActiveTrueAndCreatedAtAfter(LocalDateTime time);

    @Query("select avg(r.rating) from BookReview r where r.isActive = true")
    Double averageRatingOverall();
}
