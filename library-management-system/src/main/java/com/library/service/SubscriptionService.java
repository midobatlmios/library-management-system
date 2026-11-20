package com.library.service;

import com.library.exception.LibraryException;
import com.library.model.Subscription;
import com.library.model.User;
import com.library.payload.dto.SubscriptionDTO;
import com.library.payload.request.SubscribeRequest;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {

    /**
     * Creates a PENDING subscription and a payment (checkoutUrl). It becomes ACTIVE only when the
     * payment system confirms the payment (see PaymentTargetHandler). Free plans activate at once.
     */
    PaymentInitiateResponse subscribe(SubscribeRequest request) throws LibraryException;

    /** Starts a new PENDING subscription on the same plan; once paid it starts when the current one ends. */
    PaymentInitiateResponse renew(Long subscriptionId) throws LibraryException;

    SubscriptionDTO cancel(Long subscriptionId, String reason) throws LibraryException;

    SubscriptionDTO getById(Long subscriptionId) throws LibraryException;

    /** userId null = current user. Only admins may pass another user's id. */
    List<SubscriptionDTO> getHistory(Long userId) throws LibraryException;
    SubscriptionDTO getActive(Long userId) throws LibraryException;
    boolean hasValidSubscription(Long userId) throws LibraryException;

    PagesResponse<SubscriptionDTO> getAllValid(int page, int size) throws LibraryException;
    int deactivateExpired();

    /** For other modules (Book Loan): the subscription that gives this user borrow rights today. */
    Optional<Subscription> findValidSubscription(User user);
}
