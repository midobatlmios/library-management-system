package com.library.controller;

import com.library.domain.PaymentStatus;
import com.library.exception.LibraryException;
import com.library.model.Payment;
import com.library.payment.ChargeResult;
import com.library.payment.SimulatedPaymentGateway;
import com.library.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * The simulated gateway's checkout page: /pay/{paymentId}.
 * Opened in a browser (no login token needed: the long random paymentId is the access key, like a
 * real payment link). A real provider would host this page itself.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay")
public class CheckoutPageController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping(value = "/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable String paymentId) {
        try {
            Payment p = paymentService.getForCheckout(paymentId);
            return html(HttpStatus.OK, p.getStatus() == PaymentStatus.PENDING ? form(p, null) : result(p, null));
        } catch (LibraryException e) {
            return html(e.getStatus(), message("Payment not found", e.getMessage()));
        }
    }

    @PostMapping(value = "/{paymentId}", produces = MediaType.TEXT_HTML_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> pay(@PathVariable String paymentId,
                                      @RequestParam(defaultValue = "") String cardNumber,
                                      @RequestParam(defaultValue = "") String expiry,
                                      @RequestParam(defaultValue = "") String cvc) {
        try {
            PaymentService.CheckoutOutcome out = paymentService.payWithCard(paymentId, cardNumber, expiry, cvc);
            Payment p = out.payment();
            if (p.getStatus() == PaymentStatus.PENDING) {
                // Wrong form input or a declined card: show the form again with the reason.
                return html(out.outcome() == ChargeResult.Outcome.INVALID ? HttpStatus.BAD_REQUEST : HttpStatus.PAYMENT_REQUIRED,
                        form(p, out.message()));
            }
            return html(HttpStatus.OK, result(p, out.message()));
        } catch (LibraryException e) {
            return html(e.getStatus(), message("Payment could not be completed", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------ HTML

    private String form(Payment p, String error) {
        StringBuilder cards = new StringBuilder();
        SimulatedPaymentGateway.TEST_CARDS.forEach((number, meaning) -> cards
                .append("<tr><td><code>").append(number.replaceAll("(.{4})", "$1 ").trim())
                .append("</code></td><td>").append(esc(meaning)).append("</td></tr>"));

        return page("Pay " + p.getAmountInMajorUnits() + " " + p.getCurrency(), """
                <div class="warn">SIMULATED PAYMENT &mdash; no money moves. Never enter a real card.</div>
                <h1>%s %s</h1>
                <p class="muted">%s</p>
                <p class="muted">Payment %s &middot; expires %s</p>
                %s
                <form method="post" action="/pay/%s" autocomplete="off">
                  <label>Card number<input name="cardNumber" inputmode="numeric" placeholder="4242 4242 4242 4242" required></label>
                  <div class="row">
                    <label>Expiry (MM/YY)<input name="expiry" placeholder="12/30" required></label>
                    <label>CVC<input name="cvc" inputmode="numeric" placeholder="123" required></label>
                  </div>
                  <button type="submit">Pay %s %s</button>
                </form>
                <p class="muted"><a href="%s">Don't pay now, go back to the library</a></p>
                <h2>Test cards</h2>
                <table>%s</table>
                <p class="muted">Use any future expiry date and any 3-digit CVC.</p>
                """.formatted(
                esc(p.getAmountInMajorUnits().toPlainString()), esc(p.getCurrency()),
                esc(p.getDescription()),
                esc(p.getPaymentId()), esc(p.getExpiresAt().format(TIME)),
                error == null ? "" : "<div class=\"error\">" + esc(error) + "</div>",
                esc(p.getPaymentId()),
                esc(p.getAmountInMajorUnits().toPlainString()), esc(p.getCurrency()),
                esc(resultUrl(p)),
                cards));
    }

    private String result(Payment p, String note) {
        boolean ok = p.getStatus() == PaymentStatus.SUCCESS;
        String title = ok ? "Payment successful" : "Payment " + p.getStatus().name().toLowerCase();
        String details = ok
                ? "Paid " + p.getAmountInMajorUnits() + " " + p.getCurrency() + " with card ending "
                  + p.getCardLast4() + ". Reference " + p.getProviderTransactionId() + "."
                : (note != null ? note : p.getLastError() != null ? p.getLastError() : "");
        return page(title, """
                <h1 class="%s">%s</h1>
                <p>%s</p>
                <p class="muted">%s</p>
                <p><a class="button" href="%s">Back to the library</a></p>
                """.formatted(ok ? "ok" : "bad", esc(title), esc(p.getDescription()), esc(details),
                esc(resultUrl(p))));
    }

    /** The frontend's result page, which reads the payment's final status from the API. */
    private String resultUrl(Payment p) {
        return frontendUrl + "/payment/result?paymentId="
                + URLEncoder.encode(p.getPaymentId(), StandardCharsets.UTF_8);
    }

    private String message(String title, String text) {
        return page(title, "<h1 class=\"bad\">" + esc(title) + "</h1><p>" + esc(text) + "</p>");
    }

    private static String page(String title, String body) {
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  body{font-family:system-ui,sans-serif;background:#f4f5f7;color:#1d2129;margin:0;padding:24px 16px}
                  main{max-width:440px;margin:0 auto;background:#fff;border-radius:12px;padding:24px;box-shadow:0 1px 3px #0002}
                  h1{margin:.2em 0;font-size:1.6rem} h2{font-size:1rem;margin-top:1.6em}
                  .muted{color:#5f6673;font-size:.9rem} .ok{color:#137333} .bad{color:#b3261e}
                  .warn{background:#fff4ce;border:1px solid #f0c36d;border-radius:8px;padding:8px 10px;font-size:.85rem}
                  .error{background:#fde7e9;border:1px solid #f1a7ae;border-radius:8px;padding:8px 10px;margin:12px 0}
                  label{display:block;font-size:.85rem;margin-top:12px} .row{display:flex;gap:12px} .row label{flex:1}
                  input{display:block;width:100%%;box-sizing:border-box;padding:10px;margin-top:4px;border:1px solid #c4c7cc;border-radius:8px;font-size:1rem}
                  button{margin-top:18px;width:100%%;padding:12px;border:0;border-radius:8px;background:#1a5fd0;color:#fff;font-size:1rem;cursor:pointer}
                  table{width:100%%;font-size:.85rem;border-collapse:collapse} td{padding:4px 0;border-bottom:1px solid #eee}
                  a.button{display:block;text-align:center;margin-top:18px;padding:12px;border-radius:8px;background:#1a5fd0;color:#fff;text-decoration:none}
                </style></head>
                <body><main>%s</main></body></html>
                """.formatted(esc(title), body);
    }

    private static ResponseEntity<String> html(HttpStatus status, String body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'")
                // Not "no-referrer": with it, browsers send "Origin: null" when the card form is
                // submitted, and Spring's CORS check rejects that ("Invalid CORS request").
                // "same-origin" keeps the real origin on our own form and still sends
                // nothing to other sites.
                .header("Referrer-Policy", "same-origin")
                .body(body);
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
