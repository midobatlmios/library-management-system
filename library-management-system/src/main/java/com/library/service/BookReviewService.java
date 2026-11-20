package com.library.service;

import com.library.exception.LibraryException;
import com.library.payload.dto.BookReviewDTO;
import com.library.payload.request.CreateReviewRequest;
import com.library.payload.request.UpdateReviewRequest;
import com.library.payload.response.BookRatingStatisticsResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.ReviewAdminStatisticsResponse;
import com.library.payload.response.ReviewEligibilityResponse;

public interface BookReviewService {

    BookReviewDTO create(CreateReviewRequest request) throws LibraryException;

    /** Only the author. */
    BookReviewDTO update(Long reviewId, UpdateReviewRequest request) throws LibraryException;

    /** Soft delete: the author, or an admin (moderation). */
    void delete(Long reviewId) throws LibraryException;

    BookReviewDTO getById(Long reviewId) throws LibraryException;

    /** filter: ALL, BY_RATING (needs rating), VERIFIED_ONLY, TOP_HELPFUL */
    PagesResponse<BookReviewDTO> getForBook(Long bookId, String filter, Integer rating, int page, int size)
            throws LibraryException;

    PagesResponse<BookReviewDTO> getMyReviews(int page, int size) throws LibraryException;

    PagesResponse<BookReviewDTO> getUserReviews(Long userId, int page, int size) throws LibraryException;

    BookRatingStatisticsResponse getBookStatistics(Long bookId) throws LibraryException;

    BookReviewDTO markHelpful(Long reviewId) throws LibraryException;

    ReviewEligibilityResponse canReview(Long bookId) throws LibraryException;

    ReviewAdminStatisticsResponse getAdminStatistics();
}
