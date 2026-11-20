package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.FineDTO;
import com.library.payload.request.CreateFineRequest;
import com.library.payload.request.WaiveFineRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.HasUnpaidFinesResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.payload.response.TotalFinesResponse;
import com.library.service.FineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Amounts are in minor units (centimes): 2500 = 25.00 MAD. Responses also include the MAD value.
 * Users see and pay their own fines; admins create, record desk payments, waive and delete.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    // ------------------------------------------------------------------ create

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FineDTO> create(@Valid @RequestBody CreateFineRequest request) throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(fineService.createFine(request));
    }

    // ------------------------------------------------------------------ pay / waive

    /**
     * Starts an online payment and returns its checkoutUrl. The fine becomes PAID automatically
     * when that payment succeeds. (The old /pay/confirm endpoint is gone: users can't confirm their own payment.)
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentInitiateResponse> pay(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(fineService.initiatePayment(id));
    }

    /** Desk payment recorded by staff; partial payments allowed. */
    @PostMapping("/{id}/record-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FineDTO> recordPayment(@PathVariable Long id,
                                                 @RequestParam Long amount,
                                                 @RequestParam(required = false) String transactionId)
            throws LibraryException {
        return ResponseEntity.ok(fineService.recordPayment(id, amount, transactionId));
    }

    @PostMapping("/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FineDTO> waive(@Valid @RequestBody WaiveFineRequest request) throws LibraryException {
        return ResponseEntity.ok(fineService.waive(request));
    }

    // ------------------------------------------------------------------ read

    @GetMapping("/my")
    public ResponseEntity<PagesResponse<FineDTO>> getMyFines(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(fineService.getMyFines(status, type, page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<FineDTO>> getAllFines(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(fineService.getAllFines(userId, status, type, page, size));
    }

    @GetMapping("/book-loan/{bookLoanId}")
    public ResponseEntity<List<FineDTO>> getByBookLoan(@PathVariable Long bookLoanId) throws LibraryException {
        return ResponseEntity.ok(fineService.getByBookLoan(bookLoanId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FineDTO> getById(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(fineService.getById(id));
    }

    // ------------------------------------------------------------------ totals

    @GetMapping("/my/total-unpaid")
    public ResponseEntity<TotalFinesResponse> getMyTotalUnpaid() throws LibraryException {
        return ResponseEntity.ok(fineService.getMyTotalUnpaid());
    }

    /** The user themself or an admin. */
    @GetMapping("/statistics/user/{userId}/unpaid")
    public ResponseEntity<TotalFinesResponse> getUserTotalUnpaid(@PathVariable Long userId) throws LibraryException {
        return ResponseEntity.ok(fineService.getUserTotalUnpaid(userId));
    }

    /** The user themself or an admin. */
    @GetMapping("/statistics/user/{userId}/has-unpaid")
    public ResponseEntity<HasUnpaidFinesResponse> hasUnpaid(@PathVariable Long userId) throws LibraryException {
        return ResponseEntity.ok(fineService.hasUnpaidFines(userId));
    }

    @GetMapping("/statistics/collected")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TotalFinesResponse> getCollected() {
        return ResponseEntity.ok(fineService.getTotalCollected());
    }

    @GetMapping("/statistics/outstanding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TotalFinesResponse> getOutstanding() {
        return ResponseEntity.ok(fineService.getTotalOutstanding());
    }

    // ------------------------------------------------------------------ delete

    /** Irreversible. Meant only for correcting mistakes; prefer waive. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) throws LibraryException {
        fineService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Fine deleted", true));
    }
}
