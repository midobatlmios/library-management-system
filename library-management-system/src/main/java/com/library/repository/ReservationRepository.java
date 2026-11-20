package com.library.repository;

import com.library.domain.ReservationStatus;
import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /** The waiting queue for a book, first in line first. */
    List<Reservation> findByBookAndStatusOrderByReservedAtAscIdAsc(Book book, ReservationStatus status);

    Optional<Reservation> findFirstByBookAndStatusOrderByReservedAtAscIdAsc(Book book, ReservationStatus status);

    long countByBookAndStatus(Book book, ReservationStatus status);

    boolean existsByBookAndStatus(Book book, ReservationStatus status);

    boolean existsByUserAndBookAndStatusIn(User user, Book book, Collection<ReservationStatus> statuses);

    long countByUserAndStatusIn(User user, Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByUserAndBookAndStatus(User user, Book book, ReservationStatus status);

    /** Held copies whose pickup deadline passed. */
    List<Reservation> findByStatusAndAvailableUntilBefore(ReservationStatus status, LocalDateTime time);
}
