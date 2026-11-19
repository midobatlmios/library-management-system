package com.library.controller;


import com.library.exception.BookException;
import com.library.payload.dto.BookDTO;
import com.library.payload.request.BookSearchRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.PagesResponse;
import com.library.service.BookService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.web.servlet.function.ServerResponse.status;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<BookDTO>createBook(
            @Valid @RequestBody BookDTO bookDTO)throws BookException{
        BookDTO createdBook= bookService.createBook(bookDTO);
        return ResponseEntity.ok(createdBook);

    }
    @PostMapping("bulk")
    public ResponseEntity<?>createBooksBulk(
            @Valid @RequestBody List<BookDTO> bookDTOS)throws BookException{
        List<BookDTO> createdBooks= bookService.createBooksBulk(bookDTOS);
        return ResponseEntity.ok(createdBooks);

    }
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id)
            throws BookException{
        BookDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDTO> getBookByIsbn(@PathVariable String isbn)
            throws BookException {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO>updateBook(
            @PathVariable Long id,
            @RequestBody BookDTO bookDTO) throws BookException {
        // Errors (e.g. book not found) go to GlobalException, which returns the proper status and message.
        BookDTO updateBook = bookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(updateBook);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBook(
            @PathVariable Long id)throws BookException{
        bookService.deleteBook(id);
        return ResponseEntity.ok(new ApiResponse("book deleted successfully",true));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse> hardDeleteBook(
            @PathVariable Long id)throws BookException{
        bookService.hardDeleteBook(id);
        return ResponseEntity.ok(new ApiResponse("book deleted permanently",true));
    }
    @GetMapping
    public ResponseEntity<PagesResponse<BookDTO>> searchBooks(
            @RequestParam(required = false)Long genreId,
            @RequestParam(required = false , defaultValue = "false")Boolean availableOnly,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page ,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt")String sortBy,
            @RequestParam(defaultValue = "DESC")String sortDirection) {
        BookSearchRequest searchRequest = new BookSearchRequest();
        searchRequest.setGenreId(genreId);
        searchRequest.setAvailableOnly(availableOnly);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);

        PagesResponse<BookDTO> books = bookService.searchBookWithFilters(searchRequest);
        return ResponseEntity.ok(books);
    }
    @PostMapping("/search")
    public ResponseEntity<PagesResponse<BookDTO>> advancedSearch(
            @RequestBody BookSearchRequest searchRequest){

        PagesResponse<BookDTO> books = bookService.searchBookWithFilters(searchRequest);
        return ResponseEntity.ok(books);
    }
    @GetMapping("/stats")
    public ResponseEntity<BookStatsResponse> getBookStats(){
        long totalActive = bookService.getTotalActiveBooks();
        long totalAvailable = bookService.getTotalAvailableBooks();

        BookStatsResponse stats = new BookStatsResponse(totalActive, totalAvailable);
        return ResponseEntity.ok(stats);
    }

    @Getter

    public static class BookStatsResponse {
        private long totalActiveBooks;
        private long totalAvailableBooks;

        public BookStatsResponse(long totalActiveBooks, long totalAvailableBooks) {
            this.totalActiveBooks = totalActiveBooks;
            this.totalAvailableBooks = totalAvailableBooks;
        }
    }



}
