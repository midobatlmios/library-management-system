package com.library.service;

import com.library.domain.PaymentReferenceType;
import com.library.exception.LibraryException;
import com.library.model.Payment;
import com.library.model.User;
import com.library.payload.dto.PaymentDTO;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.payment.ChargeResult;

import java.util.Map;

public interface PaymentService {

    /**
     * Starts a payment. Any older PENDING payment for the same subscription/fine is cancelled,
     * so only one payment link works at a time.
     */
    PaymentInitiateResponse create(User payer, PaymentReferenceType type, Long referenceId,
                                   long amount, String currency, String description);

    /** Cancels PENDING payments for something that no longer needs them (fine waived, subscription cancelled...). */
    void cancelPendingFor(PaymentReferenceType type, Long referenceId, String reason);

    /** For the checkout page. Also marks the payment EXPIRED if its time is up. */
    Payment getForCheckout(String paymentId) throws LibraryException;

    /** Pays with a (test) card on the checkout page. */
    CheckoutOutcome payWithCard(String paymentId, String cardNumber, String expiry, String cvc)
            throws LibraryException;

    // ---- API ----
    PaymentDTO getById(String paymentId) throws LibraryException;
    PaymentDTO cancel(String paymentId) throws LibraryException;
    PagesResponse<PaymentDTO> getMyPayments(int page, int size) throws LibraryException;
    PagesResponse<PaymentDTO> search(Long userId, String status, String type, int page, int size) throws LibraryException;
    int expireStale();
    Map<String, Object> statistics();

    record CheckoutOutcome(Payment payment, ChargeResult.Outcome outcome, String message) {
    }
}
