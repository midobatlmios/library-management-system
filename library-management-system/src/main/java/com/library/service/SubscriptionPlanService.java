package com.library.service;

import com.library.domain.BillingCycle;
import com.library.exception.SubscriptionPlanException;
import com.library.payload.dto.SubscriptionPlanDTO;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PlanStatisticsResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionPlanService {
    SubscriptionPlanDTO createPlan(SubscriptionPlanDTO dto) throws SubscriptionPlanException;
    List<SubscriptionPlanDTO> createPlans(List<SubscriptionPlanDTO> dtos) throws SubscriptionPlanException;

    /** Non-admins only see active plans; an inactive plan is "not found" for them. */
    SubscriptionPlanDTO getPlanById(Long id, boolean includeInactive) throws SubscriptionPlanException;

    SubscriptionPlanDTO updatePlan(Long id, SubscriptionPlanDTO dto) throws SubscriptionPlanException;
    void deactivatePlan(Long id) throws SubscriptionPlanException;
    void deletePlanPermanently(Long id) throws SubscriptionPlanException;

    PagesResponse<SubscriptionPlanDTO> searchPlans(Boolean active, BillingCycle billingCycle,
                                                   BigDecimal minPrice, BigDecimal maxPrice, String title,
                                                   int page, int size, String sortBy, String sortDirection)
            throws SubscriptionPlanException;

    PlanStatisticsResponse getStatistics();
}
