package com.library.mapper;

import com.library.model.BookReview;
import com.library.payload.dto.BookReviewDTO;

import java.util.List;

public class BookReviewMapper {

    private BookReviewMapper() {
    }

    public static BookReviewDTO toDTO(BookReview r) {
        BookReviewDTO dto = new BookReviewDTO();
        dto.setId(r.getId());
        dto.setBookId(r.getBook().getId());
        dto.setBookTitle(r.getBook().getTitle());
        dto.setUserId(r.getUser().getId());
        dto.setUserFullName(r.getUser().getFullName());
        dto.setRating(r.getRating());
        dto.setTitle(r.getTitle());
        dto.setReviewText(r.getReviewText());
        dto.setIsVerifiedRead(r.getIsVerifiedRead());
        dto.setIsActive(r.getIsActive());
        dto.setHelpfulCount(r.getHelpfulCount());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }

    public static List<BookReviewDTO> toDTOList(List<BookReview> list) {
        return list.stream().map(BookReviewMapper::toDTO).toList();
    }
}
