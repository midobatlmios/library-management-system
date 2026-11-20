package com.library.mapper;

import com.library.model.Fine;
import com.library.payload.dto.FineDTO;

import java.util.List;

public class FineMapper {

    private FineMapper() {
    }

    public static FineDTO toDTO(Fine fine) {
        FineDTO dto = new FineDTO();
        dto.setId(fine.getId());
        dto.setBookLoanId(fine.getBookLoan().getId());
        dto.setBookId(fine.getBookLoan().getBook().getId());
        dto.setBookTitle(fine.getBookLoan().getBook().getTitle());
        dto.setUserId(fine.getUser().getId());
        dto.setUserEmail(fine.getUser().getEmail());
        dto.setType(fine.getType());
        dto.setStatus(fine.getStatus());
        dto.setAmount(fine.getAmount());
        dto.setAmountPaid(fine.getAmountPaid());
        dto.setAmountOutstanding(fine.getAmountOutstanding());
        dto.setAmountInMajorUnits(Fine.toMajor(fine.getAmount()));
        dto.setAmountOutstandingInMajorUnits(Fine.toMajor(fine.getAmountOutstanding()));
        dto.setCurrency(fine.getCurrency());
        dto.setReason(fine.getReason());
        dto.setNotes(fine.getNotes());
        dto.setWaivedById(fine.getWaivedBy() != null ? fine.getWaivedBy().getId() : null);
        dto.setWaivedAt(fine.getWaivedAt());
        dto.setWaiverReason(fine.getWaiverReason());
        dto.setPaidAt(fine.getPaidAt());
        dto.setProcessedById(fine.getProcessedBy() != null ? fine.getProcessedBy().getId() : null);
        dto.setTransactionId(fine.getTransactionId());
        dto.setCreatedAt(fine.getCreatedAt());
        dto.setUpdatedAt(fine.getUpdatedAt());
        return dto;
    }

    public static List<FineDTO> toDTOList(List<Fine> fines) {
        return fines.stream().map(FineMapper::toDTO).toList();
    }
}
