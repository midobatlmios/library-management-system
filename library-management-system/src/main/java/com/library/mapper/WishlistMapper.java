package com.library.mapper;

import com.library.model.Book;
import com.library.model.Wishlist;
import com.library.payload.dto.WishlistDTO;

import java.util.List;

public class WishlistMapper {

    private WishlistMapper() {
    }

    public static WishlistDTO toDTO(Wishlist w) {
        Book book = w.getBook();
        WishlistDTO dto = new WishlistDTO();
        dto.setId(w.getId());
        dto.setUserId(w.getUser().getId());
        dto.setBookId(book.getId());
        dto.setBookTitle(book.getTitle());
        dto.setBookAuthor(book.getAuthor());
        dto.setBookIsbn(book.getIsbn());
        dto.setCoverImageUrl(book.getCoverImageUrl());
        dto.setAvailableCopies(book.getAvailableCopies());
        dto.setBookActive(book.getActive());
        dto.setNotes(w.getNotes());
        dto.setAddedAt(w.getAddedAt());
        return dto;
    }

    public static List<WishlistDTO> toDTOList(List<Wishlist> list) {
        return list.stream().map(WishlistMapper::toDTO).toList();
    }
}
