package com.library.repository;

import com.library.domain.BookLoanStatus;
import com.library.model.Book;
import com.library.model.BookLoan;
import com.library.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long>, JpaSpecificationExecutor<BookLoan> {

    /** [date, number of loans that started that day] from `from` on (admin daily statistics). */
    @Query("select l.checkoutDate, count(l) from BookLoan l where l.checkoutDate >= :from group by l.checkoutDate")
    List<Object[]> countCheckoutsByDay(@Param("from") LocalDate from);

    /** [date, number of books returned that day] from `from` on. */
    @Query("select l.returnDate, count(l) from BookLoan l where l.returnDate >= :from group by l.returnDate")
    List<Object[]> countReturnsByDay(@Param("from") LocalDate from);

    long countByUserAndStatusIn(User user, Collection<BookLoanStatus> statuses);

    boolean existsByUserAndBookAndStatusIn(User user, Book book, Collection<BookLoanStatus> statuses);

    /** Has the user finished (returned) a loan of this book? Used to allow reviews. */
    boolean existsByUserAndBookAndStatus(User user, Book book, BookLoanStatus status);

    Page<BookLoan> findByUser(User user, Pageable pageable);

    Page<BookLoan> findByUserAndStatusIn(User user, Collection<BookLoanStatus> statuses, Pageable pageable);

    /** Does this user hold any book past its due date? */
    @Query("select count(l) > 0 from BookLoan l where l.user = :user and l.status in " +
            "(com.library.domain.BookLoanStatus.CHECKED_OUT, com.library.domain.BookLoanStatus.OVERDUE) " +
            "and l.dueDate < :today")
    boolean hasLateLoans(@Param("user") User user, @Param("today") LocalDate today);

    /** Checked-out loans due on this date whose reminder for that due date has not been sent yet. */
    @Query("select l from BookLoan l join fetch l.user join fetch l.book " +
            "where l.status = com.library.domain.BookLoanStatus.CHECKED_OUT and l.dueDate = :dueDate " +
            "and (l.dueReminderSentFor is null or l.dueReminderSentFor <> l.dueDate)")
    List<BookLoan> findNeedingDueReminder(@Param("dueDate") LocalDate dueDate);

    /** Not-returned loans whose due date has passed (for the overdue job). */
    @Query("select l from BookLoan l where l.status in (com.library.domain.BookLoanStatus.CHECKED_OUT, " +
            "com.library.domain.BookLoanStatus.OVERDUE) and l.dueDate < :today")
    List<BookLoan> findLateUnreturned(@Param("today") LocalDate today);

    // ---- statistics ----
    long countByStatus(BookLoanStatus status);

    long countByCheckoutDateGreaterThanEqual(LocalDate date);

    @Query("select count(l) from BookLoan l where l.status = com.library.domain.BookLoanStatus.RETURNED " +
            "and l.returnDate > l.dueDate")
    long countReturnedLate();

    @Query("select count(l) from BookLoan l where l.status = com.library.domain.BookLoanStatus.CHECKED_OUT " +
            "and l.dueDate < :today")
    long countLateButNotMarked(@Param("today") LocalDate today);
}
