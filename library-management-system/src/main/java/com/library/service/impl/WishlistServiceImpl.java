package com.library.service.impl;

import com.library.exception.BookException;
import com.library.exception.LibraryException;
import com.library.exception.UserException;
import com.library.exception.WishlistException;
import com.library.mapper.WishlistMapper;
import com.library.model.Book;
import com.library.model.User;
import com.library.model.Wishlist;
import com.library.payload.dto.WishlistDTO;
import com.library.payload.response.PagesResponse;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import com.library.repository.WishlistRepository;
import com.library.service.UserService;
import com.library.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class WishlistServiceImpl implements WishlistService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NOTES = 500;

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public WishlistDTO add(Long bookId, String notes) throws LibraryException {
        User user = userService.getCurrentUser();
        Book book = findBook(bookId);
        if (!book.getActive()) {
            throw new BookException("Book not found with id: " + bookId, HttpStatus.NOT_FOUND);
        }
        if (wishlistRepository.existsByUserAndBook(user, book)) {
            throw new WishlistException("Book already exists in wishlist", HttpStatus.CONFLICT);
        }
        Wishlist item = Wishlist.builder()
                .user(user)
                .book(book)
                .notes(cleanNotes(notes))
                .build();
        return WishlistMapper.toDTO(wishlistRepository.save(item));
    }

    @Override
    public void remove(Long bookId) throws LibraryException {
        wishlistRepository.delete(findMine(bookId));
    }

    @Override
    public WishlistDTO updateNotes(Long bookId, String notes) throws LibraryException {
        Wishlist item = findMine(bookId);
        item.setNotes(cleanNotes(notes));
        return WishlistMapper.toDTO(wishlistRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<WishlistDTO> getMyWishlist(int page, int size) throws LibraryException {
        return pageOf(userService.getCurrentUser(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<WishlistDTO> getUserWishlist(Long userId, int page, int size) throws LibraryException {
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !current.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
        return pageOf(user, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInMyWishlist(Long bookId) throws LibraryException {
        return wishlistRepository.existsByUserAndBook(userService.getCurrentUser(), findBook(bookId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countMine() throws LibraryException {
        return wishlistRepository.countByUser(userService.getCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public long countForBook(Long bookId) throws LibraryException {
        return wishlistRepository.countByBook(findBook(bookId));
    }

    // ------------------------------------------------------------------ helpers

    private Wishlist findMine(Long bookId) throws LibraryException {
        User user = userService.getCurrentUser();
        return wishlistRepository.findByUserAndBook(user, findBook(bookId))
                .orElseThrow(() -> new WishlistException(
                        "Book " + bookId + " is not in your wishlist", HttpStatus.NOT_FOUND));
    }

    private Book findBook(Long bookId) throws BookException {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookException("Book not found with id: " + bookId, HttpStatus.NOT_FOUND));
    }

    private PagesResponse<WishlistDTO> pageOf(User user, int page, int size) throws WishlistException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new WishlistException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<Wishlist> p = wishlistRepository.findByUser(user,
                PageRequest.of(page, size, Sort.by("addedAt").descending().and(Sort.by("id").descending())));
        return new PagesResponse<>(WishlistMapper.toDTOList(p.getContent()),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                p.isLast(), p.isFirst(), p.isEmpty());
    }

    private static String cleanNotes(String notes) throws WishlistException {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String trimmed = notes.trim();
        if (trimmed.length() > MAX_NOTES) {
            throw new WishlistException("Notes must be at most " + MAX_NOTES + " characters");
        }
        return trimmed;
    }
}
