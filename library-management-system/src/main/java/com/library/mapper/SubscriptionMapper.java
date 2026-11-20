package com.library.mapper;

import com.library.model.Subscription;
import com.library.payload.dto.SubscriptionDTO;

import java.util.List;

public class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionDTO toDTO(Subscription s) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUser().getId());
        dto.setUserEmail(s.getUser().getEmail());
        dto.setPlanId(s.getPlan().getId());
        dto.setPlanName(s.getPlanName());
        dto.setPlanCode(s.getPlanCode());
        dto.setPrice(s.getPrice());
        dto.setPriceInMajorUnits(s.getPriceInMajorUnits());
        dto.setCurrency(s.getCurrency());
        dto.setStatus(s.getStatus());
        dto.setValid(s.isValid());
        dto.setDaysRemaining(s.getDaysRemaining());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setDurationDays(s.getDurationDays());
        dto.setMaxBooksAllowed(s.getMaxBooksAllowed());
        dto.setMaxBorrowDays(s.getMaxBorrowDays());
        dto.setAutoRenew(s.getAutoRenew());
        dto.setRenewedFromId(s.getRenewedFromId());
        dto.setActivatedAt(s.getActivatedAt());
        dto.setCancelledAt(s.getCancelledAt());
        dto.setCancellationReason(s.getCancellationReason());
        dto.setNotes(s.getNotes());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    public static List<SubscriptionDTO> toDTOList(List<Subscription> subs) {
        return subs.stream().map(SubscriptionMapper::toDTO).toList();
    }
}
