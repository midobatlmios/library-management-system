package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.BookReviewDTO;
import com.library.payload.request.CreateReviewRequest;
import com.library.payload.request.UpdateReviewRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.BookRatingStatisticsResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.ReviewAdminStatisticsResponse;
import com.library.payload.response.ReviewEligibilityResponse;
import com.library.service.BookReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Reading reviews is public (book pages, user pages). Writing, voting and "my reviews" need a login.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class BookReviewController {

    private final BookReviewService reviewService;

    @PostMapping
    public ResponseEntity<BookReviewDTO> create(@Valid @RequestBody CreateReviewRequest request)
            throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<BookReviewDTO> update(@PathVariable Long reviewId,
                                                @Valid @RequestBody UpdateReviewRequest request)
            throws LibraryException {
        return ResponseEntity.ok(reviewService.update(reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long reviewId) throws LibraryException {
        reviewService.delete(reviewId);
        return ResponseEntity.ok(new ApiResponse("Review deleted successfully", true));
    }

    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<BookReviewDTO> markHelpful(@PathVariable Long reviewId) throws LibraryException {
        return ResponseEntity.ok(reviewService.markHelpful(reviewId));
    }

    @GetMapping("/can-review/{bookId}")
    public ResponseEntity<ReviewEligibilityResponse> canReview(@PathVariable Long bookId) throws LibraryException {
        return ResponseEntity.ok(reviewService.canReview(bookId));
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<PagesResponse<BookReviewDTO>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws LibraryException {
        return ResponseEntity.ok(reviewService.getMyReviews(page, size));
    }

    // ------------------------------------------------------------------ public

    /** filter = ALL | BY_RATING (with rating=1..5) | VERIFIED_ONLY | TOP_HELPFUL */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<PagesResponse<BookReviewDTO>> getForBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws LibraryException {
        return ResponseEntity.ok(reviewService.getForBook(bookId, filter, rating, page, size));
    }

    @GetMapping("/book/{bookId}/statistics")
    public ResponseEntity<BookRatingStatisticsResponse> getBookStatistics(@PathVariable Long bookId)
            throws LibraryException {
        return ResponseEntity.ok(reviewService.getBookStatistics(bookId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagesResponse<BookReviewDTO>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws LibraryException {
        return ResponseEntity.ok(reviewService.getUserReviews(userId, page, size));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<BookReviewDTO> getById(@PathVariable Long reviewId) throws LibraryException {
        return ResponseEntity.ok(reviewService.getById(reviewId));
    }

    // ------------------------------------------------------------------ admin

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewAdminStatisticsResponse> getAdminStatistics() {
        return ResponseEntity.ok(reviewService.getAdminStatistics());
    }
}
