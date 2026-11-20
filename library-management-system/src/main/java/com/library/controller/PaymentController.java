package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.PaymentDTO;
import com.library.payload.response.PagesResponse;
import com.library.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment history and status. Paying itself happens on the checkoutUrl (/pay/{paymentId}).
 * There is deliberately NO endpoint here to mark a payment as paid.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/my")
    public ResponseEntity<PagesResponse<PaymentDTO>> getMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(paymentService.getMyPayments(page, size));
    }

    /** The payer or an admin. The frontend polls this after sending the user to checkout. */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getById(@PathVariable String paymentId) throws LibraryException {
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentDTO> cancel(@PathVariable String paymentId) throws LibraryException {
        return ResponseEntity.ok(paymentService.cancel(paymentId));
    }

    // ------------------------------------------------------------------ admin

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<PaymentDTO>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(paymentService.search(userId, status, type, page, size));
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(paymentService.statistics());
    }

    /** Unpaid payments past their time limit -> EXPIRED. Step 13 runs this automatically. */
    @PostMapping("/admin/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> expire() {
        int count = paymentService.expireStale();
        return ResponseEntity.ok(Map.of("message", count + " payment(s) expired", "success", true, "count", count));
    }
}
