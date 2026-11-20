package com.library.repository;

import com.library.domain.SubscriptionStatus;
import com.library.model.Subscription;
import com.library.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserOrderByCreatedAtDesc(User user);

    List<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);

    boolean existsByUserAndStatusIn(User user, List<SubscriptionStatus> statuses);

    /** Has this user ever had a paid subscription? (Trial days are only for the first one.) */
    boolean existsByUserAndActivatedAtIsNotNull(User user);

    /** The subscription that gives borrow rights today. */
    @Query("select s from Subscription s where s.user = :user and s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.startDate <= :today and s.endDate >= :today order by s.endDate desc")
    List<Subscription> findValid(@Param("user") User user, @Param("today") LocalDate today);

    /** A paid renewal that starts after the current one ends. */
    @Query("select count(s) > 0 from Subscription s where s.user = :user and s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.startDate > :today")
    boolean hasQueuedRenewal(@Param("user") User user, @Param("today") LocalDate today);

    /** Latest end date among paid, not-yet-ended subscriptions (to start a renewal right after it). */
    @Query("select max(s.endDate) from Subscription s where s.user = :user and s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.endDate >= :today")
    Optional<LocalDate> findLatestActiveEndDate(@Param("user") User user, @Param("today") LocalDate today);

    @Query(value = "select s from Subscription s join fetch s.user where s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.startDate <= :today and s.endDate >= :today",
            countQuery = "select count(s) from Subscription s where s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
                    "and s.startDate <= :today and s.endDate >= :today")
    Page<Subscription> findAllValid(@Param("today") LocalDate today, Pageable pageable);

    @Modifying
    @Query("update Subscription s set s.status = com.library.domain.SubscriptionStatus.EXPIRED where s.status = com.library.domain.SubscriptionStatus.ACTIVE and s.endDate < :today")
    int expireEndedSubscriptions(@Param("today") LocalDate today);

    // ---- statistics for plans ----
    @Query("select count(s) from Subscription s where s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.startDate <= :today and s.endDate >= :today")
    long countValid(@Param("today") LocalDate today);

    /** Rows of [planCode, count] */
    @Query("select s.planCode, count(s) from Subscription s where s.status = com.library.domain.SubscriptionStatus.ACTIVE " +
            "and s.startDate <= :today and s.endDate >= :today group by s.planCode")
    List<Object[]> countValidByPlan(@Param("today") LocalDate today);
}
