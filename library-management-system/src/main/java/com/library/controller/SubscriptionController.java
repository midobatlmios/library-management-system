package com.library.controller;

import com.library.exception.LibraryException;
import com.library.payload.dto.SubscriptionDTO;
import com.library.payload.request.SubscribeRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Every endpoint needs a logged-in user. Users act on their own subscriptions;
 * admins can act on anyone's (pass ?userId= where supported).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Creates a PENDING subscription and returns a payment with a checkoutUrl.
     * The subscription becomes ACTIVE automatically when that payment succeeds.
     * (The old POST /activate endpoint is gone: users can no longer confirm their own payment.)
     */
    @PostMapping("/subscribe")
    public ResponseEntity<PaymentInitiateResponse> subscribe(@Valid @RequestBody SubscribeRequest request)
            throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.subscribe(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionDTO>> getHistory(@RequestParam(required = false) Long userId)
            throws LibraryException {
        return ResponseEntity.ok(subscriptionService.getHistory(userId));
    }

    @GetMapping("/user/active")
    public ResponseEntity<SubscriptionDTO> getActive(@RequestParam(required = false) Long userId)
            throws LibraryException {
        return ResponseEntity.ok(subscriptionService.getActive(userId));
    }

    /** Used before issuing a book. Always 200; "success" says whether the subscription is valid. */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse> check(@RequestParam(required = false) Long userId) throws LibraryException {
        boolean valid = subscriptionService.hasValidSubscription(userId);
        return ResponseEntity.ok(new ApiResponse(
                valid ? "User has valid subscription" : "User has no valid subscription", valid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDTO> getById(@PathVariable Long id) throws LibraryException {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @PostMapping("/renew/{subscriptionId}")
    public ResponseEntity<PaymentInitiateResponse> renew(@PathVariable Long subscriptionId) throws LibraryException {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.renew(subscriptionId));
    }

    @PostMapping("/cancel/{subscriptionId}")
    public ResponseEntity<SubscriptionDTO> cancel(@PathVariable Long subscriptionId,
                                                  @RequestParam(required = false) String reason)
            throws LibraryException {
        return ResponseEntity.ok(subscriptionService.cancel(subscriptionId, reason));
    }

    // ------------------------------------------------------------------ admin

    @GetMapping("/admin/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagesResponse<SubscriptionDTO>> getAllActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws LibraryException {
        return ResponseEntity.ok(subscriptionService.getAllValid(page, size));
    }

    /** Marks ended subscriptions as EXPIRED. Step 13 runs this automatically every night. */
    @PostMapping("/admin/deactivate-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateExpired() {
        int count = subscriptionService.deactivateExpired();
        return ResponseEntity.ok(Map.of(
                "message", count + " expired subscription(s) deactivated",
                "success", true,
                "count", count));
    }
}
