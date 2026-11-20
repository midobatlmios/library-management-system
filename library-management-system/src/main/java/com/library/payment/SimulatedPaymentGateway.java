package com.library.payment;

import com.library.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A fake payment provider for development and demos. No money moves and no card data is stored.
 *
 * Test cards (any future expiry date, any 3-digit CVC):
 *   4242 4242 4242 4242  -> success
 *   4000 0000 0000 0002  -> declined
 *   4000 0000 0000 9995  -> insufficient funds
 * Any other valid-looking number is declined, so a real card is never "charged".
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    public static final Map<String, String> TEST_CARDS = Map.of(
            "4242424242424242", "Always succeeds",
            "4000000000000002", "Always declined",
            "4000000000009995", "Insufficient funds");

    @Value("${app.payment.checkout-base-url:http://localhost:9090/pay}")
    private String checkoutBaseUrl;

    @Override
    public String providerName() {
        return "SIMULATED";
    }

    @Override
    public String checkoutUrl(Payment payment) {
        return checkoutBaseUrl + "/" + payment.getPaymentId();
    }

    /** Validates the form like a real gateway would, then applies the test-card rules. */
    public ChargeResult charge(String cardNumber, String expiry, String cvc) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{16}") || !luhnValid(digits)) {
            return ChargeResult.invalid("Card number is not valid");
        }
        if (!validExpiry(expiry)) {
            return ChargeResult.invalid("Expiry date must be MM/YY and not in the past");
        }
        if (cvc == null || !cvc.trim().matches("\\d{3}")) {
            return ChargeResult.invalid("CVC must be 3 digits");
        }

        String last4 = digits.substring(12);
        return switch (digits) {
            case "4242424242424242" -> ChargeResult.success(
                    "SIMTXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT),
                    last4);
            case "4000000000009995" -> ChargeResult.declined("Insufficient funds", last4);
            case "4000000000000002" -> ChargeResult.declined("Your card was declined", last4);
            default -> ChargeResult.declined("Simulated gateway: please use one of the test cards", last4);
        };
    }

    private static boolean validExpiry(String expiry) {
        if (expiry == null || !expiry.trim().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        String[] parts = expiry.trim().split("/");
        YearMonth exp = YearMonth.of(2000 + Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
        return !exp.isBefore(YearMonth.now());
    }

    /** Standard card-number checksum. */
    private static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
