package com.library.repository;

import com.library.domain.BillingCycle;
import com.library.model.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    boolean existsByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

    boolean existsByPlanCode(String planCode);
    boolean existsByPlanCodeAndIdNot(String planCode, Long id);

    long countByActiveTrue();

    /** Every filter is optional: a null parameter means "don't filter on this". */
    @Query("select p from SubscriptionPlan p where " +
            "(:active is null or p.active = :active) and " +
            "(:billingCycle is null or p.billingCycle = :billingCycle) and " +
            "(:minPrice is null or p.price >= :minPrice) and " +
            "(:maxPrice is null or p.price <= :maxPrice) and " +
            "(:title is null or lower(p.title) like lower(concat('%', :title, '%')))")
    Page<SubscriptionPlan> searchPlans(
            @Param("active") Boolean active,
            @Param("billingCycle") BillingCycle billingCycle,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("title") String title,
            Pageable pageable);
}
