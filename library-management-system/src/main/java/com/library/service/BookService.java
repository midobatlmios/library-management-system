package com.library.service;

import com.library.exception.BookException;
import com.library.payload.dto.BookDTO;
import com.library.payload.request.BookSearchRequest;
import com.library.payload.response.PagesResponse;

import java.util.List;

public interface BookService {

    BookDTO createBook(BookDTO bookDTO) throws BookException;
    List<BookDTO> createBooksBulk(List<BookDTO> bookDTOs) throws BookException;
    BookDTO getBookById(Long bookId) throws BookException;
    BookDTO getBookByIsbn(String isbn) throws BookException;
    BookDTO updateBook(Long id, BookDTO bookDTO) throws BookException;
    void deleteBook(Long bookId) throws BookException;
    void hardDeleteBook(Long bookId) throws BookException;

    PagesResponse<BookDTO> searchBookWithFilters(
            BookSearchRequest searchRequest
    );


    long getTotalActiveBooks();

    long getTotalAvailableBooks();
}
