package com.library.service;

import com.library.domain.ReservationStatus;
import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The waiting queue for each book. Used by Book Loan (returns, checkout, renew) and by Reservation.
 * It only depends on repositories, so both services can use it without depending on each other.
 *
 * Copy rule: a returned copy goes to the first PENDING reservation (it is "held" and does NOT go
 * back to availableCopies). Only when nobody is waiting does it go back on the shelf.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationQueueService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final EmailService emailService;

    /** How long a held copy waits for pickup. */
    @Value("${app.reservation.pickup-hours:48}")
    private long pickupHours;

    /** Is anyone waiting for a copy of this book? */
    @Transactional(readOnly = true)
    public boolean hasWaiting(Book book) {
        return reservationRepository.existsByBookAndStatus(book, ReservationStatus.PENDING);
    }

    /** A copy held for this user, if any. */
    @Transactional(readOnly = true)
    public Optional<Reservation> findHeldFor(User user, Book book) {
        return reservationRepository.findFirstByUserAndBookAndStatus(user, book, ReservationStatus.AVAILABLE);
    }

    /**
     * A copy came back (book returned, or a held copy was released).
     * Holds it for the next person in line, or puts it back on the shelf.
     */
    public void onCopyFreed(Book book) {
        Optional<Reservation> next = reservationRepository
                .findFirstByBookAndStatusOrderByReservedAtAscIdAsc(book, ReservationStatus.PENDING);
        if (next.isEmpty()) {
            bookRepository.returnOneCopy(book.getId());
            return;
        }
        Reservation r = next.get();
        LocalDateTime now = LocalDateTime.now();
        r.setStatus(ReservationStatus.AVAILABLE);
        r.setAvailableAt(now);
        r.setAvailableUntil(now.plusHours(pickupHours));
        r.setQueuePosition(0);
        emailService.sendReservationReady(r);     // sent after this transaction commits
        log.info("Reservation {}: a copy is held until {}", r.getId(), r.getAvailableUntil());
        r.setNotificationSent(true);
        reservationRepository.save(r);
        recomputeQueue(book);
    }

    /** The user borrowed their held copy. */
    public void markFulfilled(Reservation r, Long loanId) {
        r.setStatus(ReservationStatus.FULFILLED);
        r.setFulfilledAt(LocalDateTime.now());
        r.setFulfilledLoanId(loanId);
        r.setQueuePosition(null);
        reservationRepository.save(r);
    }

    /**
     * Closes an active reservation (cancel or expire). If it was holding a copy,
     * that copy moves on to the next person, or back to the shelf.
     */
    public void close(Reservation r, ReservationStatus newStatus, String note) {
        boolean wasHoldingCopy = r.getStatus() == ReservationStatus.AVAILABLE;
        r.setStatus(newStatus);
        r.setCancelledAt(LocalDateTime.now());
        r.setQueuePosition(null);
        if (note != null && !note.isBlank()) {
            r.setNotes(note);
        }
        reservationRepository.save(r);

        if (wasHoldingCopy) {
            onCopyFreed(r.getBook());
        } else {
            recomputeQueue(r.getBook());
        }
    }

    /** Renumbers the PENDING reservations of a book: 1, 2, 3... by reservation time. */
    public void recomputeQueue(Book book) {
        List<Reservation> waiting = reservationRepository
                .findByBookAndStatusOrderByReservedAtAscIdAsc(book, ReservationStatus.PENDING);
        for (int i = 0; i < waiting.size(); i++) {
            waiting.get(i).setQueuePosition(i + 1);
        }
        reservationRepository.saveAll(waiting);
    }
}
