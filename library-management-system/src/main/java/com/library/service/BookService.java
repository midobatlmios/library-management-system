package com.library.service;

import com.library.exception.BookException;
import com.library.payload.dto.BookDTO;
import com.library.payload.request.BookSearchRequest;
import com.library.payload.response.PagesResponse;

import java.util.List;

public interface BookService {

    BookDTO createBook(BookDTO bookDTO) throws BookException;

    /** All or nothing. */
    List<BookDTO> createBooksBulk(List<BookDTO> bookDTOs) throws BookException;

    /** includeInactive: admins can see soft-deleted books, everyone else gets 404. */
    BookDTO getBookById(Long bookId, boolean includeInactive) throws BookException;

    BookDTO getBookByIsbn(String isbn, boolean includeInactive) throws BookException;

    BookDTO updateBook(Long id, BookDTO bookDTO) throws BookException;

    void deleteBook(Long bookId) throws BookException;

    void hardDeleteBook(Long bookId) throws BookException;

    PagesResponse<BookDTO> searchBookWithFilters(BookSearchRequest searchRequest) throws BookException;

    long getTotalActiveBooks();

    long getTotalAvailableBooks();
}
