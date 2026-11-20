package com.library.payment;

import com.library.model.Payment;

/**
 * A payment provider. Today only {@link SimulatedPaymentGateway} exists.
 * A real provider (Stripe, PayPal, CMI...) would implement this, return its own hosted checkout URL,
 * and confirm payments from its webhook by calling PaymentService.completeSuccess / completeDecline.
 */
public interface PaymentGateway {

    /** Short name stored on each payment, e.g. SIMULATED, STRIPE. */
    String providerName();

    /** Where the user goes to pay this payment. */
    String checkoutUrl(Payment payment);
}
