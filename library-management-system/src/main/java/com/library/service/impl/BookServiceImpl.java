package com.library.service.impl;

import com.library.exception.BookException;
import com.library.mapper.BookMapper;
import com.library.model.Book;
import com.library.model.Genre;
import com.library.payload.dto.BookDTO;
import com.library.payload.request.BookSearchRequest;
import com.library.payload.response.PagesResponse;
import com.library.repository.BookRepository;
import com.library.repository.GenreRepository;
import com.library.service.BookService;
import com.library.service.ReservationQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = BookException.class)
public class BookServiceImpl implements BookService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "author", "createdAt", "publicationDate", "price", "availableCopies");
    private static final int MAX_PAGE_SIZE = 100;

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final BookMapper bookMapper;
    private final ReservationQueueService reservationQueue;

    // ------------------------------------------------------------------ create

    @Override
    public BookDTO createBook(BookDTO dto) throws BookException {
        return bookMapper.toDTO(bookRepository.save(buildNew(dto)));
    }

    @Override
    public List<BookDTO> createBooksBulk(List<BookDTO> dtos) throws BookException {
        if (dtos == null || dtos.isEmpty()) {
            throw new BookException("At least one book is required");
        }
        Set<String> isbns = new HashSet<>();
        List<Book> books = new ArrayList<>();
        for (BookDTO dto : dtos) {
            Book book = buildNew(dto);
            if (!isbns.add(book.getIsbn())) {
                throw new BookException("ISBN " + book.getIsbn() + " appears twice in the request", HttpStatus.CONFLICT);
            }
            books.add(book);
        }
        List<BookDTO> result = new ArrayList<>();
        for (Book saved : bookRepository.saveAll(books)) {
            result.add(bookMapper.toDTO(saved));
        }
        return result;
    }

    private Book buildNew(BookDTO dto) throws BookException {
        validateIsbn(dto.getIsbn());
        if (bookRepository.existsByIsbn(dto.getIsbn().trim())) {
            throw new BookException("Book with ISBN " + dto.getIsbn().trim() + " already exists", HttpStatus.CONFLICT);
        }
        int total = dto.getTotalCopies();
        int available = dto.getAvailableCopies() != null ? dto.getAvailableCopies() : total;
        if (available > total) {
            throw new BookException("Available copies (" + available + ") cannot exceed total copies (" + total + ")");
        }
        Book book = new Book();
        bookMapper.copyDetails(dto, book, findActiveGenre(dto.getGenreId()));
        book.setTotalCopies(total);
        book.setAvailableCopies(available);
        book.setActive(true);
        return book;
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public BookDTO getBookById(Long bookId, boolean includeInactive) throws BookException {
        Book book = findBook(bookId);
        if (!includeInactive && !book.getActive()) {
            throw notFound("id: " + bookId);
        }
        return bookMapper.toDTO(book);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDTO getBookByIsbn(String isbn, boolean includeInactive) throws BookException {
        Book book = bookRepository.findByIsbn(isbn.trim())
                .filter(b -> includeInactive || b.getActive())
                .orElseThrow(() -> notFound("ISBN: " + isbn));
        return bookMapper.toDTO(book);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookDTO> searchBookWithFilters(BookSearchRequest r) throws BookException {
        int page = r.getPage() == null ? 0 : r.getPage();
        int size = r.getSize() == null ? 20 : r.getSize();
        String sortBy = r.getSortBy() == null ? "createdAt" : r.getSortBy();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BookException("Cannot sort by '" + sortBy + "'. Allowed: " + SORTABLE_FIELDS);
        }
        Sort sort = "ASC".equalsIgnoreCase(r.getSortDirection())
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort.and(Sort.by("id").ascending()));
        String term = r.getSearchTerm() == null || r.getSearchTerm().isBlank() ? null : r.getSearchTerm().trim();

        Page<Book> books = bookRepository.saerchBooksWithFilter(
                term, r.getGenreId(), Boolean.TRUE.equals(r.getAvailableOnly()), pageable);
        return new PagesResponse<>(books.getContent().stream().map(bookMapper::toDTO).toList(),
                books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(),
                books.isLast(), books.isFirst(), books.isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalActiveBooks() {
        return bookRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalAvailableBooks() {
        return bookRepository.countAvailableBooks();
    }

    // ------------------------------------------------------------------ update / delete

    /**
     * Full update. availableCopies in the request is ignored: when totalCopies changes, the shelf
     * count moves by the same amount. New copies go first to users waiting in the reservation queue.
     */
    @Override
    public BookDTO updateBook(Long bookId, BookDTO dto) throws BookException {
        Book book = findBook(bookId);

        String isbn = dto.getIsbn().trim();
        if (!isbn.equals(book.getIsbn())) {
            validateIsbn(isbn);
            if (bookRepository.existsByIsbn(isbn)) {
                throw new BookException("Book with ISBN " + isbn + " already exists", HttpStatus.CONFLICT);
            }
        }

        int newTotal = dto.getTotalCopies();
        int delta = newTotal - book.getTotalCopies();
        if (delta < 0 && -delta > book.getAvailableCopies()) {
            throw new BookException("Cannot reduce total copies to " + newTotal + ": "
                    + book.getCopiesOut() + " copies are on loan or held for reservations", HttpStatus.CONFLICT);
        }

        bookMapper.copyDetails(dto, book, findActiveGenre(dto.getGenreId()));
        if (dto.getActive() != null) {
            book.setActive(dto.getActive());
        }
        book.setTotalCopies(newTotal);
        if (delta < 0) {
            book.setAvailableCopies(book.getAvailableCopies() + delta);
        }
        for (int i = 0; i < delta; i++) {
            if (reservationQueue.hasWaiting(book)) {
                reservationQueue.onCopyFreed(book);       // held for the next person in line
            } else {
                book.setAvailableCopies(book.getAvailableCopies() + 1);
            }
        }
        return bookMapper.toDTO(bookRepository.save(book));
    }

    /** Soft delete: hidden from the catalog; loans and history stay. Restore with PUT "active": true. */
    @Override
    public void deleteBook(Long bookId) throws BookException {
        Book book = findBook(bookId);
        book.setActive(false);
        bookRepository.save(book);
    }

    /** Permanent. Refused while loans, reservations, reviews, wishlists or fines refer to the book. */
    @Override
    public void hardDeleteBook(Long bookId) throws BookException {
        Book book = findBook(bookId);
        try {
            bookRepository.delete(book);
            bookRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BookException("This book has history (loans, reservations, reviews or wishlists) "
                    + "and cannot be deleted permanently. Use soft delete instead.", HttpStatus.CONFLICT);
        }
    }

    // ------------------------------------------------------------------ helpers

    private Book findBook(Long id) throws BookException {
        return bookRepository.findById(id).orElseThrow(() -> notFound("id: " + id));
    }

    private static BookException notFound(String what) {
        return new BookException("Book not found with " + what, HttpStatus.NOT_FOUND);
    }

    private Genre findActiveGenre(Long genreId) throws BookException {
        return genreRepository.findById(genreId)
                .filter(Genre::getActive)
                .orElseThrow(() -> new BookException("Genre not found with id: " + genreId, HttpStatus.NOT_FOUND));
    }

    /** ISBN-10 (last char may be X) or ISBN-13, ignoring hyphens and spaces. */
    private static void validateIsbn(String isbn) throws BookException {
        String digits = isbn.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{13}") && !digits.matches("\\d{9}[\\dXx]")) {
            throw new BookException("ISBN must be a valid ISBN-10 or ISBN-13, e.g. 978-0-13-468599-1");
        }
    }
}
