package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Returned when something needs to be paid (subscribe, renew, pay a fine).
 * Send the user to checkoutUrl; the payment system confirms the payment itself.
 * status is PENDING normally, or SUCCESS when nothing had to be paid (a free plan).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {
    private String paymentId;
    /** SUBSCRIPTION or FINE */
    private String referenceType;
    private Long referenceId;
    private String description;
    /** Minor units, e.g. 19900 */
    private Long amount;
    /** Major units, e.g. 199.00 */
    private BigDecimal amountInMajorUnits;
    private String currency;
    private String status;
    private String checkoutUrl;
    private LocalDateTime expiresAt;
    private String message;
}
