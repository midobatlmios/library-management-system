package com.library.service.impl;

import com.library.domain.BookLoanStatus;
import com.library.domain.ReservationStatus;
import com.library.exception.BookException;
import com.library.exception.LibraryException;
import com.library.exception.ReservationException;
import com.library.exception.UserException;
import com.library.mapper.ReservationMapper;
import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.payload.dto.ReservationDTO;
import com.library.payload.request.CheckoutRequest;
import com.library.payload.request.ReservationRequest;
import com.library.payload.request.ReservationSearchRequest;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.QueuePositionResponse;
import com.library.repository.BookLoanRepository;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import com.library.repository.UserRepository;
import com.library.service.BookLoanService;
import com.library.service.ReservationQueueService;
import com.library.service.ReservationService;
import com.library.service.SubscriptionService;
import com.library.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class ReservationServiceImpl implements ReservationService {

    private static final Set<ReservationStatus> ACTIVE =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.AVAILABLE);
    private static final Set<BookLoanStatus> NOT_RETURNED =
            EnumSet.of(BookLoanStatus.CHECKED_OUT, BookLoanStatus.OVERDUE);
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "reservedAt", "availableUntil", "status", "queuePosition", "updatedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final ReservationRepository reservationRepository;
    private final ReservationQueueService queue;
    private final BookRepository bookRepository;
    private final BookLoanRepository loanRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final BookLoanService loanService;

    @Value("${app.reservation.max-active-per-user:5}")
    private int maxActivePerUser;

    // ------------------------------------------------------------------ create / cancel / fulfill

    @Override
    public ReservationDTO create(ReservationRequest request, Long userId) throws LibraryException {
        User user = userId == null ? userService.getCurrentUser() : findUser(userId);

        Book book = bookRepository.findById(request.getBookId())
                .filter(Book::getActive)
                .orElseThrow(() -> new BookException(
                        "Book not found with id: " + request.getBookId(), HttpStatus.NOT_FOUND));

        if (subscriptionService.findValidSubscription(user).isEmpty()) {
            throw new ReservationException("An active subscription is required to reserve books");
        }
        if (loanRepository.existsByUserAndBookAndStatusIn(user, book, NOT_RETURNED)) {
            throw new ReservationException("You already have '" + book.getTitle() + "' on loan",
                    HttpStatus.CONFLICT);
        }
        if (reservationRepository.existsByUserAndBookAndStatusIn(user, book, ACTIVE)) {
            throw new ReservationException("You already have an active reservation for '" + book.getTitle() + "'",
                    HttpStatus.CONFLICT);
        }
        if (book.getAvailableCopies() > 0) {
            throw new ReservationException("'" + book.getTitle()
                    + "' has copies available right now. Borrow it directly instead of reserving.");
        }
        if (reservationRepository.countByUserAndStatusIn(user, ACTIVE) >= maxActivePerUser) {
            throw new ReservationException("You can have at most " + maxActivePerUser + " active reservations");
        }

        Reservation r = Reservation.builder()
                .user(user)
                .book(book)
                .status(ReservationStatus.PENDING)
                .queuePosition((int) reservationRepository.countByBookAndStatus(book, ReservationStatus.PENDING) + 1)
                .notes(request.getNotes())
                .build();
        return ReservationMapper.toDTO(reservationRepository.save(r));
    }

    @Override
    public ReservationDTO cancel(Long reservationId, String reason) throws LibraryException {
        Reservation r = findOwned(reservationId);
        if (!r.canBeCancelled()) {
            throw new ReservationException("Reservation is already " + r.getStatus(), HttpStatus.CONFLICT);
        }
        queue.close(r, ReservationStatus.CANCELLED,
                reason == null || reason.isBlank() ? "Cancelled by user" : reason.trim());
        return ReservationMapper.toDTO(r);
    }

    @Override
    public ReservationDTO fulfill(Long reservationId) throws LibraryException {
        Reservation r = findOwned(reservationId);
        if (r.getStatus() != ReservationStatus.AVAILABLE) {
            throw new ReservationException(r.getStatus() == ReservationStatus.PENDING
                    ? "No copy is ready yet. Your place in the queue: " + r.getQueuePosition()
                    : "Reservation is already " + r.getStatus(), HttpStatus.CONFLICT);
        }
        if (r.hasExpired()) {
            throw new ReservationException("The pickup deadline (" + r.getAvailableUntil() + ") has passed",
                    HttpStatus.CONFLICT);
        }

        // Normal checkout: it finds the held copy, checks subscription, limits and fines,
        // creates the loan and marks this reservation FULFILLED.
        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setBookId(r.getBook().getId());
        loanService.checkout(checkout, r.getUser().getId());

        return ReservationMapper.toDTO(r);
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public ReservationDTO getById(Long reservationId) throws LibraryException {
        return ReservationMapper.toDTO(findOwned(reservationId));
    }

    @Override
    @Transactional(readOnly = true)
    public QueuePositionResponse getQueuePosition(Long reservationId) throws LibraryException {
        Reservation r = findOwned(reservationId);
        String message = switch (r.getStatus()) {
            case PENDING -> {
                int ahead = r.getQueuePosition() - 1;
                yield ahead == 0 ? "You are next in line"
                        : "There are " + ahead + " person(s) ahead of you";
            }
            case AVAILABLE -> "A copy is waiting for you. Pick it up before " + r.getAvailableUntil();
            default -> "This reservation is " + r.getStatus();
        };
        return new QueuePositionResponse(r.getId(), r.getStatus(), r.getQueuePosition(),
                r.getAvailableUntil(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<ReservationDTO> searchMine(ReservationSearchRequest request) throws LibraryException {
        request.setUserId(userService.getCurrentUser().getId());
        return search(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<ReservationDTO> searchAll(ReservationSearchRequest request) throws LibraryException {
        return search(request);
    }

    private PagesResponse<ReservationDTO> search(ReservationSearchRequest req) throws ReservationException {
        if (req.getPage() < 0 || req.getSize() < 1 || req.getSize() > MAX_PAGE_SIZE) {
            throw new ReservationException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(req.getSortBy())) {
            throw new ReservationException("Cannot sort by '" + req.getSortBy() + "'. Allowed: " + SORTABLE_FIELDS);
        }
        ReservationStatus status = parseStatus(req.getStatus());

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (req.getUserId() != null) p.add(cb.equal(root.get("user").get("id"), req.getUserId()));
            if (req.getBookId() != null) p.add(cb.equal(root.get("book").get("id"), req.getBookId()));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (Boolean.TRUE.equals(req.getActiveOnly())) p.add(root.get("status").in(ACTIVE));
            if (req.getReservedFrom() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("reservedAt"), req.getReservedFrom().atStartOfDay()));
            if (req.getReservedTo() != null)
                p.add(cb.lessThan(root.get("reservedAt"), req.getReservedTo().plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(Predicate[]::new));
        };

        Sort sort = "ASC".equalsIgnoreCase(req.getSortDirection())
                ? Sort.by(req.getSortBy()).ascending() : Sort.by(req.getSortBy()).descending();
        Page<Reservation> page = reservationRepository.findAll(spec,
                PageRequest.of(req.getPage(), req.getSize(), sort.and(Sort.by("id").descending())));
        return new PagesResponse<>(ReservationMapper.toDTOList(page.getContent()),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isLast(), page.isFirst(), page.isEmpty());
    }

    // ------------------------------------------------------------------ admin job

    @Override
    public int expireUncollected() {
        List<Reservation> expired = reservationRepository.findByStatusAndAvailableUntilBefore(
                ReservationStatus.AVAILABLE, LocalDateTime.now());
        for (Reservation r : expired) {
            queue.close(r, ReservationStatus.EXPIRED, "Not picked up before " + r.getAvailableUntil());
        }
        return expired.size();
    }

    // ------------------------------------------------------------------ helpers

    private Reservation findOwned(Long reservationId) throws LibraryException {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(
                        "Reservation not found with id: " + reservationId, HttpStatus.NOT_FOUND));
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !r.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return r;
    }

    private User findUser(Long userId) throws UserException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
    }

    private static ReservationStatus parseStatus(String status) throws ReservationException {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ReservationException("Unknown status '" + status + "'. Allowed: "
                    + java.util.Arrays.toString(ReservationStatus.values()));
        }
    }
}
