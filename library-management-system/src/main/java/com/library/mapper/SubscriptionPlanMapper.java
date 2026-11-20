package com.library.mapper;

import com.library.model.SubscriptionPlan;
import com.library.payload.dto.SubscriptionPlanDTO;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionPlanMapper {

    private SubscriptionPlanMapper() {
    }

    public static SubscriptionPlanDTO toDTO(SubscriptionPlan plan) {
        SubscriptionPlanDTO dto = new SubscriptionPlanDTO();
        dto.setId(plan.getId());
        dto.setPlanCode(plan.getPlanCode());
        dto.setTitle(plan.getTitle());
        dto.setDescription(plan.getDescription());
        dto.setPrice(plan.getPrice());
        dto.setCurrency(plan.getCurrency());
        dto.setDurationInDays(plan.getDurationInDays());
        dto.setBillingCycle(plan.getBillingCycle());
        dto.setTrialDays(plan.getTrialDays());
        dto.setMaxBooksAllowed(plan.getMaxBooksAllowed());
        dto.setMaxBorrowDays(plan.getMaxBorrowDays());
        dto.setFeatures(new ArrayList<>(plan.getFeatures()));
        dto.setAutoRenew(plan.getAutoRenew());
        dto.setActive(plan.getActive());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    public static List<SubscriptionPlanDTO> toDTOList(List<SubscriptionPlan> plans) {
        return plans.stream().map(SubscriptionPlanMapper::toDTO).toList();
    }

    /**
     * Copies the DTO onto the entity. Fields the client left out (null) keep their current value,
     * so on create they keep the entity defaults and on update they stay unchanged.
     */
    public static void copyToEntity(SubscriptionPlanDTO dto, SubscriptionPlan plan) {
        if (dto.getPlanCode() != null) plan.setPlanCode(dto.getPlanCode());
        plan.setTitle(dto.getTitle().trim());
        if (dto.getDescription() != null) plan.setDescription(dto.getDescription());
        plan.setPrice(dto.getPrice());
        if (dto.getCurrency() != null) plan.setCurrency(dto.getCurrency());
        plan.setDurationInDays(dto.getDurationInDays());
        if (dto.getBillingCycle() != null) plan.setBillingCycle(dto.getBillingCycle());
        if (dto.getTrialDays() != null) plan.setTrialDays(dto.getTrialDays());
        if (dto.getMaxBooksAllowed() != null) plan.setMaxBooksAllowed(dto.getMaxBooksAllowed());
        if (dto.getMaxBorrowDays() != null) plan.setMaxBorrowDays(dto.getMaxBorrowDays());
        if (dto.getFeatures() != null) {
            plan.getFeatures().clear();
            plan.getFeatures().addAll(dto.getFeatures());
        }
        if (dto.getAutoRenew() != null) plan.setAutoRenew(dto.getAutoRenew());
        if (dto.getActive() != null) plan.setActive(dto.getActive());
    }
}
