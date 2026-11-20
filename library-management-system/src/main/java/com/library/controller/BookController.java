package com.library.controller;

import com.library.exception.BookException;
import com.library.payload.dto.BookDTO;
import com.library.payload.request.BookSearchRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.PagesResponse;
import com.library.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Browsing the catalog is public. Adding, editing and deleting books is admin only.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    // ------------------------------------------------------------------ admin

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookDTO bookDTO) throws BookException {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(bookDTO));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookDTO>> createBooksBulk(@RequestBody List<@Valid BookDTO> bookDTOs)
            throws BookException {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBooksBulk(bookDTOs));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @Valid @RequestBody BookDTO bookDTO)
            throws BookException {
        return ResponseEntity.ok(bookService.updateBook(id, bookDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteBook(@PathVariable Long id) throws BookException {
        bookService.deleteBook(id);
        return ResponseEntity.ok(new ApiResponse("Book deleted successfully", true));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> hardDeleteBook(@PathVariable Long id) throws BookException {
        bookService.hardDeleteBook(id);
        return ResponseEntity.ok(new ApiResponse("Book permanently deleted", true));
    }

    // ------------------------------------------------------------------ public

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) throws BookException {
        return ResponseEntity.ok(bookService.getBookById(id, isAdmin()));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDTO> getBookByIsbn(@PathVariable String isbn) throws BookException {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn, isAdmin()));
    }

    /** Example: /api/books?search=java&genreId=1&availableOnly=true&sortBy=title&sortDirection=ASC */
    @GetMapping
    public ResponseEntity<PagesResponse<BookDTO>> searchBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long genreId,
            @RequestParam(defaultValue = "false") Boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "false") Boolean includeInactive) throws BookException {
        return ResponseEntity.ok(bookService.searchBookWithFilters(
                new BookSearchRequest(search, genreId, availableOnly, page, size, sortBy, sortDirection,
                        includeInactive && isAdmin())));   // only admins may see hidden books
    }

    @PostMapping("/search")
    public ResponseEntity<PagesResponse<BookDTO>> advancedSearch(@RequestBody BookSearchRequest searchRequest)
            throws BookException {
        searchRequest.setIncludeInactive(Boolean.TRUE.equals(searchRequest.getIncludeInactive()) && isAdmin());
        return ResponseEntity.ok(bookService.searchBookWithFilters(searchRequest));
    }

    @GetMapping("/stats")
    public ResponseEntity<BookStatsResponse> getBookStats() {
        return ResponseEntity.ok(new BookStatsResponse(
                bookService.getTotalActiveBooks(), bookService.getTotalAvailableBooks()));
    }

    public record BookStatsResponse(long totalActiveBooks, long totalAvailableBooks) {
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
