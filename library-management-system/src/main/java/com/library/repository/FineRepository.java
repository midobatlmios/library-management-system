package com.library.repository;

import com.library.domain.FineStatus;
import com.library.domain.FineType;
import com.library.model.BookLoan;
import com.library.model.Fine;
import com.library.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByBookLoanOrderByCreatedAtDesc(BookLoan bookLoan);

    boolean existsByBookLoanAndType(BookLoan bookLoan, FineType type);

    /** Every filter is optional: null means "don't filter". */
    @Query("select f from Fine f where " +
            "(:userId is null or f.user.id = :userId) and " +
            "(:status is null or f.status = :status) and " +
            "(:type is null or f.type = :type)")
    Page<Fine> search(@Param("userId") Long userId,
                      @Param("status") FineStatus status,
                      @Param("type") FineType type,
                      Pageable pageable);

    // ---- money owed / collected (minor units) ----

    @Query("select count(f) > 0 from Fine f where f.user = :user and f.status in " +
            "(com.library.domain.FineStatus.PENDING, com.library.domain.FineStatus.PARTIALLY_PAID)")
    boolean hasUnpaid(@Param("user") User user);

    @Query("select coalesce(sum(f.amount - f.amountPaid), 0) from Fine f where f.user = :user and f.status in " +
            "(com.library.domain.FineStatus.PENDING, com.library.domain.FineStatus.PARTIALLY_PAID)")
    long sumOutstandingForUser(@Param("user") User user);

    @Query("select coalesce(sum(f.amount - f.amountPaid), 0) from Fine f where f.status in " +
            "(com.library.domain.FineStatus.PENDING, com.library.domain.FineStatus.PARTIALLY_PAID)")
    long sumOutstanding();

    /** Everything actually received, including partial payments. */
    @Query("select coalesce(sum(f.amountPaid), 0) from Fine f")
    long sumCollected();
}
