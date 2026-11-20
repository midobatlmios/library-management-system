package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.WishlistDTO;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.CountResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.WishlistCheckResponse;
import com.library.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Everything here needs a login. Users manage their own wishlist; admins can view anyone's. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    /** Example: POST /api/wishlist/add/18?notes=read after finals */
    @PostMapping("/add/{bookId}")
    public ResponseEntity<WishlistDTO> add(@PathVariable Long bookId,
                                           @RequestParam(required = false) String notes) throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.add(bookId, notes));
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<ApiResponse> remove(@PathVariable Long bookId) throws LibraryException {
        wishlistService.remove(bookId);
        return ResponseEntity.ok(new ApiResponse("Book removed from wishlist successfully", true));
    }

    @PutMapping("/update-notes/{bookId}")
    public ResponseEntity<WishlistDTO> updateNotes(@PathVariable Long bookId,
                                                   @RequestParam(required = false) String notes)
            throws LibraryException {
        return ResponseEntity.ok(wishlistService.updateNotes(bookId, notes));
    }

    @GetMapping("/my-wishlist")
    public ResponseEntity<PagesResponse<WishlistDTO>> getMyWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws LibraryException {
        return ResponseEntity.ok(wishlistService.getMyWishlist(page, size));
    }

    /** The user themself or an admin: a wishlist is personal. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PagesResponse<WishlistDTO>> getUserWishlist(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws LibraryException {
        return ResponseEntity.ok(wishlistService.getUserWishlist(userId, page, size));
    }

    @GetMapping("/check/{bookId}")
    public ResponseEntity<WishlistCheckResponse> check(@PathVariable Long bookId) throws LibraryException {
        return ResponseEntity.ok(new WishlistCheckResponse(wishlistService.isInMyWishlist(bookId)));
    }

    @GetMapping("/my-count")
    public ResponseEntity<CountResponse> myCount() throws LibraryException {
        return ResponseEntity.ok(new CountResponse(wishlistService.countMine()));
    }

    /** How many users saved this book (a popularity signal). */
    @GetMapping("/book/{bookId}/count")
    public ResponseEntity<CountResponse> bookCount(@PathVariable Long bookId) throws LibraryException {
        return ResponseEntity.ok(new CountResponse(wishlistService.countForBook(bookId)));
    }
}
