package com.library.service.impl;

import com.library.domain.BookLoanStatus;
import com.library.exception.BookException;
import com.library.exception.BookReviewException;
import com.library.exception.LibraryException;
import com.library.exception.UserException;
import com.library.mapper.BookReviewMapper;
import com.library.model.Book;
import com.library.model.BookReview;
import com.library.model.ReviewHelpfulVote;
import com.library.model.User;
import com.library.payload.dto.BookReviewDTO;
import com.library.payload.request.CreateReviewRequest;
import com.library.payload.request.UpdateReviewRequest;
import com.library.payload.response.BookRatingStatisticsResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.ReviewAdminStatisticsResponse;
import com.library.payload.response.ReviewEligibilityResponse;
import com.library.repository.BookLoanRepository;
import com.library.repository.BookRepository;
import com.library.repository.BookReviewRepository;
import com.library.repository.ReviewHelpfulVoteRepository;
import com.library.repository.UserRepository;
import com.library.service.BookReviewService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class BookReviewServiceImpl implements BookReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository voteRepository;
    private final BookRepository bookRepository;
    private final BookLoanRepository loanRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // ------------------------------------------------------------------ create / update / delete

    @Override
    public BookReviewDTO create(CreateReviewRequest request) throws LibraryException {
        User user = userService.getCurrentUser();
        Book book = findBook(request.getBookId());

        String notAllowed = whyCannotReview(user, book);
        if (notAllowed != null) {
            throw new BookReviewException(notAllowed,
                    notAllowed.startsWith("You already") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST);
        }

        // One row per user and book: a review the user deleted earlier is reused.
        BookReview review = reviewRepository.findByUserAndBook(user, book).orElseGet(BookReview::new);
        if (review.getId() != null) {
            voteRepository.deleteByReview(review);
        }
        review.setUser(user);
        review.setBook(book);
        review.setRating(request.getRating());
        review.setTitle(trimOrNull(request.getTitle()));
        review.setReviewText(request.getReviewText().trim());
        review.setIsVerifiedRead(hasReturnedLoan(user, book));
        review.setIsActive(true);
        review.setHelpfulCount(0);

        return BookReviewMapper.toDTO(reviewRepository.save(review));
    }

    @Override
    public BookReviewDTO update(Long reviewId, UpdateReviewRequest request) throws LibraryException {
        BookReview review = findActive(reviewId);
        User current = userService.getCurrentUser();
        if (!review.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the author can edit this review");
        }
        review.setRating(request.getRating());
        review.setTitle(trimOrNull(request.getTitle()));
        review.setReviewText(request.getReviewText().trim());
        return BookReviewMapper.toDTO(reviewRepository.save(review));
    }

    @Override
    public void delete(Long reviewId) throws LibraryException {
        BookReview review = findActive(reviewId);
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !review.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        review.setIsActive(false);
        reviewRepository.save(review);
    }

    // ------------------------------------------------------------------ read (public)

    @Override
    @Transactional(readOnly = true)
    public BookReviewDTO getById(Long reviewId) throws LibraryException {
        return BookReviewMapper.toDTO(findActive(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookReviewDTO> getForBook(Long bookId, String filter, Integer rating, int page, int size)
            throws LibraryException {
        Book book = findBook(bookId);
        String f = filter == null || filter.isBlank() ? "ALL" : filter.trim().toUpperCase(Locale.ROOT);

        Sort newest = Sort.by("createdAt").descending().and(Sort.by("id").descending());
        Page<BookReview> result = switch (f) {
            case "ALL" -> reviewRepository.findByBookAndIsActiveTrue(book, pageable(page, size, newest));
            case "VERIFIED_ONLY" ->
                    reviewRepository.findByBookAndIsActiveTrueAndIsVerifiedReadTrue(book, pageable(page, size, newest));
            case "TOP_HELPFUL" -> reviewRepository.findByBookAndIsActiveTrue(book,
                    pageable(page, size, Sort.by("helpfulCount").descending().and(newest)));
            case "BY_RATING" -> {
                if (rating == null || rating < 1 || rating > 5) {
                    throw new BookReviewException("filter=BY_RATING needs rating between 1 and 5");
                }
                yield reviewRepository.findByBookAndIsActiveTrueAndRating(book, rating, pageable(page, size, newest));
            }
            default -> throw new BookReviewException(
                    "Unknown filter '" + filter + "'. Use ALL, BY_RATING, VERIFIED_ONLY or TOP_HELPFUL");
        };
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookReviewDTO> getMyReviews(int page, int size) throws LibraryException {
        return toPage(reviewRepository.findByUserAndIsActiveTrue(userService.getCurrentUser(),
                pageable(page, size, Sort.by("createdAt").descending())));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookReviewDTO> getUserReviews(Long userId, int page, int size) throws LibraryException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
        return toPage(reviewRepository.findByUserAndIsActiveTrue(user,
                pageable(page, size, Sort.by("createdAt").descending())));
    }

    @Override
    @Transactional(readOnly = true)
    public BookRatingStatisticsResponse getBookStatistics(Long bookId) throws LibraryException {
        Book book = findBook(bookId);

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(String.valueOf(stars), 0L);
        }
        for (Object[] row : reviewRepository.ratingDistribution(book)) {
            distribution.put(String.valueOf(row[0]), (Long) row[1]);
        }

        return new BookRatingStatisticsResponse(
                book.getId(),
                book.getTitle(),
                round(reviewRepository.averageRating(book)),
                reviewRepository.countByBookAndIsActiveTrue(book),
                distribution,
                reviewRepository.countByBookAndIsActiveTrueAndIsVerifiedReadTrue(book));
    }

    // ------------------------------------------------------------------ helpful votes / eligibility

    @Override
    public BookReviewDTO markHelpful(Long reviewId) throws LibraryException {
        BookReview review = findActive(reviewId);
        User user = userService.getCurrentUser();

        if (review.getUser().getId().equals(user.getId())) {
            throw new BookReviewException("You cannot mark your own review as helpful");
        }
        if (voteRepository.existsByReviewAndUser(review, user)) {
            throw new BookReviewException("You already marked this review as helpful", HttpStatus.CONFLICT);
        }
        voteRepository.save(ReviewHelpfulVote.builder().review(review).user(user).build());
        reviewRepository.incrementHelpful(reviewId);

        return BookReviewMapper.toDTO(findActive(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse canReview(Long bookId) throws LibraryException {
        String reason = whyCannotReview(userService.getCurrentUser(), findBook(bookId));
        return new ReviewEligibilityResponse(reason == null, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewAdminStatisticsResponse getAdminStatistics() {
        return new ReviewAdminStatisticsResponse(
                reviewRepository.countByIsActiveTrue(),
                reviewRepository.countByIsActiveFalse(),
                reviewRepository.countByIsActiveTrueAndIsVerifiedReadTrue(),
                round(reviewRepository.averageRatingOverall()),
                reviewRepository.countByIsActiveTrueAndCreatedAtAfter(LocalDateTime.now().minusDays(30)));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Null when the user may review; otherwise the reason.
     * Readers must have returned a loan of the book. Admins (librarians) may review without one;
     * their review is then not marked as a verified read.
     */
    private String whyCannotReview(User user, Book book) {
        if (reviewRepository.existsByUserAndBookAndIsActiveTrue(user, book)) {
            return "You already reviewed this book. Edit your existing review instead.";
        }
        if (!user.isAdmin() && !hasReturnedLoan(user, book)) {
            return "Only readers who borrowed and returned this book can review it";
        }
        return null;
    }

    private boolean hasReturnedLoan(User user, Book book) {
        return loanRepository.existsByUserAndBookAndStatus(user, book, BookLoanStatus.RETURNED);
    }

    private Book findBook(Long bookId) throws BookException {
        return bookRepository.findById(bookId)
                .filter(Book::getActive)
                .orElseThrow(() -> new BookException("Book not found with id: " + bookId, HttpStatus.NOT_FOUND));
    }

    private BookReview findActive(Long reviewId) throws BookReviewException {
        return reviewRepository.findById(reviewId)
                .filter(BookReview::getIsActive)
                .orElseThrow(() -> new BookReviewException(
                        "Review not found with id: " + reviewId, HttpStatus.NOT_FOUND));
    }

    private static Pageable pageable(int page, int size, Sort sort) throws BookReviewException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookReviewException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    private static PagesResponse<BookReviewDTO> toPage(Page<BookReview> p) {
        return new PagesResponse<>(BookReviewMapper.toDTOList(p.getContent()),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                p.isLast(), p.isFirst(), p.isEmpty());
    }

    private static BigDecimal round(Double value) {
        return value == null ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
