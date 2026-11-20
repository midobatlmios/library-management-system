package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.ReservationDTO;
import com.library.payload.request.ReservationRequest;
import com.library.payload.request.ReservationSearchRequest;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.QueuePositionResponse;
import com.library.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Reserve a book with no copies on the shelf, wait in the queue, then pick up the held copy.
 * Users manage their own reservations; admins can see and act on all of them.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationDTO> create(@Valid @RequestBody ReservationRequest request)
            throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, null));
    }

    /** Librarian places a hold for a user. */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationDTO> createForUser(@PathVariable Long userId,
                                                       @Valid @RequestBody ReservationRequest request)
            throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, userId));
    }

    /** Only PENDING or AVAILABLE reservations. A held copy moves on to the next person. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationDTO> cancel(@PathVariable Long id,
                                                 @RequestParam(required = false) String reason)
            throws LibraryException {
        return ResponseEntity.ok(reservationService.cancel(id, reason));
    }

    /** Borrow the held copy (creates the book loan). */
    @PostMapping("/{id}/fulfill")
    public ResponseEntity<ReservationDTO> fulfill(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(reservationService.fulfill(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getById(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/{id}/queue-position")
    public ResponseEntity<QueuePositionResponse> getQueuePosition(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(reservationService.getQueuePosition(id));
    }

    // ------------------------------------------------------------------ the logged-in user

    @GetMapping("/my")
    public ResponseEntity<PagesResponse<ReservationDTO>> getMine(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reservedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) throws LibraryException {
        return ResponseEntity.ok(reservationService.searchMine(
                toSearch(null, null, status, activeOnly, null, null, page, size, sortBy, sortDirection)));
    }

    @PostMapping("/my/search")
    public ResponseEntity<PagesResponse<ReservationDTO>> searchMine(@RequestBody ReservationSearchRequest request)
            throws LibraryException {
        return ResponseEntity.ok(reservationService.searchMine(request));
    }

    // ------------------------------------------------------------------ admin

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<ReservationDTO>> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) LocalDate reservedFrom,
            @RequestParam(required = false) LocalDate reservedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reservedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) throws LibraryException {
        return ResponseEntity.ok(reservationService.searchAll(toSearch(userId, bookId, status, activeOnly,
                reservedFrom, reservedTo, page, size, sortBy, sortDirection)));
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<ReservationDTO>> searchAll(@RequestBody ReservationSearchRequest request)
            throws LibraryException {
        return ResponseEntity.ok(reservationService.searchAll(request));
    }

    /** Held copies not picked up in time. Step 13 runs this automatically. */
    @PostMapping("/admin/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> expire() {
        int count = reservationService.expireUncollected();
        return ResponseEntity.ok(Map.of(
                "message", count + " reservation(s) expired",
                "success", true,
                "count", count));
    }

    private static ReservationSearchRequest toSearch(Long userId, Long bookId, String status, Boolean activeOnly,
                                                     LocalDate from, LocalDate to, int page, int size,
                                                     String sortBy, String sortDirection) {
        ReservationSearchRequest r = new ReservationSearchRequest();
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setStatus(status);
        r.setActiveOnly(activeOnly);
        r.setReservedFrom(from);
        r.setReservedTo(to);
        r.setPage(page);
        r.setSize(size);
        r.setSortBy(sortBy);
        r.setSortDirection(sortDirection);
        return r;
    }
}
