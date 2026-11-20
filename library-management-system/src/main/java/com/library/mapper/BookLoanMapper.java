package com.library.mapper;

import com.library.model.BookLoan;
import com.library.payload.dto.BookLoanDTO;

import java.util.List;

public class BookLoanMapper {

    private BookLoanMapper() {
    }

    public static BookLoanDTO toDTO(BookLoan loan) {
        BookLoanDTO dto = new BookLoanDTO();
        dto.setId(loan.getId());
        dto.setUserId(loan.getUser().getId());
        dto.setUserEmail(loan.getUser().getEmail());
        dto.setUserFullName(loan.getUser().getFullName());
        dto.setBookId(loan.getBook().getId());
        dto.setBookTitle(loan.getBook().getTitle());
        dto.setBookAuthor(loan.getBook().getAuthor());
        dto.setBookIsbn(loan.getBook().getIsbn());
        dto.setType(loan.getType());
        dto.setStatus(loan.getStatus());
        dto.setCheckoutDate(loan.getCheckoutDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnDate(loan.getReturnDate());
        dto.setRenewalCount(loan.getRenewalCount());
        dto.setMaxRenewals(loan.getMaxRenewals());
        dto.setRenewalsLeft(Math.max(0, loan.getMaxRenewals() - loan.getRenewalCount()));
        dto.setIsOverdue(loan.isLate());
        dto.setOverdueDays(loan.getCurrentOverdueDays());
        dto.setNotes(loan.getNotes());
        dto.setCreatedAt(loan.getCreatedAt());
        dto.setUpdatedAt(loan.getUpdatedAt());
        return dto;
    }

    public static List<BookLoanDTO> toDTOList(List<BookLoan> loans) {
        return loans.stream().map(BookLoanMapper::toDTO).toList();
    }
}
