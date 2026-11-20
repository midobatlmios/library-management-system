package com.library.payment;

/**
 * Result of one card attempt.
 * INVALID = the form was filled in wrong (not counted as an attempt), DECLINED = the card was refused.
 */
public record ChargeResult(Outcome outcome, String transactionId, String message, String cardLast4) {

    public enum Outcome { SUCCESS, DECLINED, INVALID }

    public static ChargeResult success(String transactionId, String last4) {
        return new ChargeResult(Outcome.SUCCESS, transactionId, "Payment successful", last4);
    }

    public static ChargeResult declined(String message, String last4) {
        return new ChargeResult(Outcome.DECLINED, null, message, last4);
    }

    public static ChargeResult invalid(String message) {
        return new ChargeResult(Outcome.INVALID, null, message, null);
    }
}
