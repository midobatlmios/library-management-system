package com.library.mapper;

import com.library.model.Reservation;
import com.library.payload.dto.ReservationDTO;

import java.util.List;

public class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(r.getId());
        dto.setUserId(r.getUser().getId());
        dto.setUserEmail(r.getUser().getEmail());
        dto.setUserFullName(r.getUser().getFullName());
        dto.setBookId(r.getBook().getId());
        dto.setBookTitle(r.getBook().getTitle());
        dto.setBookIsbn(r.getBook().getIsbn());
        dto.setStatus(r.getStatus());
        dto.setActive(r.isActive());
        dto.setQueuePosition(r.getQueuePosition());
        dto.setReservedAt(r.getReservedAt());
        dto.setAvailableAt(r.getAvailableAt());
        dto.setAvailableUntil(r.getAvailableUntil());
        dto.setFulfilledAt(r.getFulfilledAt());
        dto.setCancelledAt(r.getCancelledAt());
        dto.setNotificationSent(r.getNotificationSent());
        dto.setFulfilledLoanId(r.getFulfilledLoanId());
        dto.setNotes(r.getNotes());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }

    public static List<ReservationDTO> toDTOList(List<Reservation> list) {
        return list.stream().map(ReservationMapper::toDTO).toList();
    }
}
