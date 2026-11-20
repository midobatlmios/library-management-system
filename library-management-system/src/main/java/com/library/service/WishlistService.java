package com.library.service;

import com.library.exception.LibraryException;
import com.library.payload.dto.WishlistDTO;
import com.library.payload.response.PagesResponse;

public interface WishlistService {
    WishlistDTO add(Long bookId, String notes) throws LibraryException;
    void remove(Long bookId) throws LibraryException;
    WishlistDTO updateNotes(Long bookId, String notes) throws LibraryException;
    PagesResponse<WishlistDTO> getMyWishlist(int page, int size) throws LibraryException;
    /** The user themself or an admin. */
    PagesResponse<WishlistDTO> getUserWishlist(Long userId, int page, int size) throws LibraryException;
    boolean isInMyWishlist(Long bookId) throws LibraryException;
    long countMine() throws LibraryException;
    long countForBook(Long bookId) throws LibraryException;
}
