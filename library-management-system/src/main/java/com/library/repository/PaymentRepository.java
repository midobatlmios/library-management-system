package com.library.repository;

import com.library.domain.PaymentReferenceType;
import com.library.domain.PaymentStatus;
import com.library.model.Payment;
import com.library.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    /**
     * Locks the row until the transaction ends, so the same payment can't be processed twice at once.
     * Plain SQL on purpose: works on both MySQL and MariaDB (Hibernate's own lock syntax fails on MariaDB).
     */
    @Query(value = "select * from payments where payment_id = :paymentId for update", nativeQuery = true)
    Optional<Payment> lockByPaymentId(@Param("paymentId") String paymentId);

    List<Payment> findByReferenceTypeAndReferenceIdAndStatus(PaymentReferenceType type, Long referenceId,
                                                            PaymentStatus status);

    Page<Payment> findByUser(User user, Pageable pageable);

    @Query("select p from Payment p where " +
            "(:userId is null or p.user.id = :userId) and " +
            "(:status is null or p.status = :status) and " +
            "(:type is null or p.referenceType = :type)")
    Page<Payment> search(@Param("userId") Long userId,
                         @Param("status") PaymentStatus status,
                         @Param("type") PaymentReferenceType type,
                         Pageable pageable);

    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime time);

    // ---- statistics (minor units) ----
    @Query("select coalesce(sum(p.amount), 0) from Payment p " +
            "where p.status = com.library.domain.PaymentStatus.SUCCESS and p.referenceType = :type")
    long sumSucceeded(@Param("type") PaymentReferenceType type);

    long countByStatus(PaymentStatus status);

    /** [completedAt, amount in minor units] of every successful payment since `from`. */
    @Query("select p.completedAt, p.amount from Payment p " +
            "where p.status = com.library.domain.PaymentStatus.SUCCESS and p.completedAt >= :from")
    List<Object[]> findSucceededSince(@Param("from") LocalDateTime from);
}
