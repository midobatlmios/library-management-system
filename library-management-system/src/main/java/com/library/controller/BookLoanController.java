package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.BookLoanDTO;
import com.library.payload.request.CheckinRequest;
import com.library.payload.request.CheckoutRequest;
import com.library.payload.request.LoanSearchRequest;
import com.library.payload.request.RenewLoanRequest;
import com.library.payload.request.UpdateLoanRequest;
import com.library.payload.response.LoanStatisticsResponse;
import com.library.payload.response.PagesResponse;
import com.library.service.BookLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Users borrow, return, renew and view their own loans. Admins can do it for anyone,
 * plus search, corrections, the overdue job and statistics.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book-loans")
public class BookLoanController {

    private final BookLoanService loanService;

    @PostMapping("/checkout")
    public ResponseEntity<BookLoanDTO> checkout(@Valid @RequestBody CheckoutRequest request) throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.checkout(request, null));
    }

    @PostMapping("/checkout/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookLoanDTO> checkoutForUser(@PathVariable Long userId,
                                                       @Valid @RequestBody CheckoutRequest request)
            throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.checkout(request, userId));
    }

    @PostMapping("/check-in")
    public ResponseEntity<BookLoanDTO> checkin(@Valid @RequestBody CheckinRequest request) throws LibraryException {
        return ResponseEntity.ok(loanService.checkin(request));
    }

    @PostMapping("/renew")
    public ResponseEntity<BookLoanDTO> renew(@Valid @RequestBody RenewLoanRequest request) throws LibraryException {
        return ResponseEntity.ok(loanService.renew(request));
    }

    @GetMapping("/my")
    public ResponseEntity<PagesResponse<BookLoanDTO>> getMyLoans(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(loanService.getMyLoans(status, page, size));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<BookLoanDTO>> getUserLoans(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(loanService.getUserLoans(userId, status, page, size));
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<BookLoanDTO>> search(@RequestBody LoanSearchRequest request)
            throws LibraryException {
        return ResponseEntity.ok(loanService.search(request));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(loanService.getStatistics());
    }

    /** Step 13 runs this automatically every night. */
    @PostMapping("/admin/update-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateOverdue() {
        int count = loanService.updateOverdueLoans();
        return ResponseEntity.ok(Map.of(
                "message", count + " overdue loan(s) updated",
                "success", true,
                "count", count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookLoanDTO> getById(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookLoanDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateLoanRequest request) throws LibraryException {
        return ResponseEntity.ok(loanService.update(id, request));
    }
}
