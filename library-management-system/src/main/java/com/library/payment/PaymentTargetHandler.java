package com.library.payment;

import com.library.domain.PaymentReferenceType;
import com.library.exception.LibraryException;
import com.library.model.Payment;

/**
 * Implemented by the modules that can be paid for (Subscription, Fine).
 * The payment service calls these, so payments never reach into those modules directly.
 */
public interface PaymentTargetHandler {

    PaymentReferenceType type();

    /** Called before charging: throws if this payment is no longer needed (already paid, cancelled, amount changed). */
    void checkStillPayable(Payment payment) throws LibraryException;

    /** Called after a successful charge, in the same transaction: activate the subscription / clear the fine. */
    void onPaymentSucceeded(Payment payment) throws LibraryException;
}
