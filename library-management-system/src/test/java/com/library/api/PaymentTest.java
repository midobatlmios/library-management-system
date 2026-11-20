package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** The simulated payment system and the rule that users can't confirm their own payments. */
class PaymentTest extends TestSupport {

    private MvcResult subscribe(TestUser u, Long plan) throws Exception {
        MvcResult r = call(post("/api/subscriptions/subscribe"), u.token(), "{\"planId\":%d}".formatted(plan));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return r;
    }

    private String paymentStatus(String paymentId) throws Exception {
        return json(call(get("/api/payments/" + paymentId), adminToken(), null), "$.status");
    }

    private boolean canBorrow(TestUser u) throws Exception {
        return json(call(get("/api/subscriptions/check"), u.token(), null), "$.success");
    }

    @Test
    void usersCanNoLongerConfirmTheirOwnPayments() throws Exception {
        TestUser u = newUser();
        assertThat(status(post("/api/subscriptions/activate?subscriptionId=1&paymentId=x"), u.token(), null))
                .isNotEqualTo(200);
        assertThat(status(post("/api/fines/1/pay/confirm?paymentId=x"), u.token(), null)).isEqualTo(404);
    }

    @Test
    void unverifiedEmailCannotSubscribe() throws Exception {
        String email = "unverified" + unique() + "@example.com";
        String token = json(call(post("/auth/signup"), null,
                "{\"fullName\":\"U\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(email)), "$.jwt");
        assertThat(status(post("/api/subscriptions/subscribe"), token,
                "{\"planId\":%d}".formatted(newPlan(1, 14, "10")))).isEqualTo(403);
    }

    @Test
    void subscriptionIsActiveOnlyAfterASuccessfulPayment() throws Exception {
        TestUser u = newUser();
        String paymentId = json(subscribe(u, newPlan(3, 14, "199")), "$.paymentId");
        assertThat(canBorrow(u)).isFalse();

        assertThat(payWithCard(paymentId, TEST_CARD_OK)).isEqualTo(200);

        assertThat(canBorrow(u)).isTrue();
        MvcResult p = call(get("/api/payments/" + paymentId), u.token(), null);
        assertThat((String) json(p, "$.status")).isEqualTo("SUCCESS");
        assertThat((String) json(p, "$.cardLast4")).isEqualTo("4242");
    }

    @Test
    void declinedCardsKeepThePaymentOpenThenCloseItAfterThreeTries() throws Exception {
        TestUser u = newUser();
        String paymentId = json(subscribe(u, newPlan(3, 14, "199")), "$.paymentId");

        assertThat(payWithCard(paymentId, TEST_CARD_DECLINED)).isEqualTo(402);
        assertThat(paymentStatus(paymentId)).isEqualTo("PENDING");
        assertThat(payWithCard(paymentId, "1234567890123456")).as("invalid number, not counted").isEqualTo(400);
        payWithCard(paymentId, TEST_CARD_DECLINED);
        payWithCard(paymentId, TEST_CARD_DECLINED);

        assertThat(paymentStatus(paymentId)).isEqualTo("FAILED");
        assertThat(canBorrow(u)).isFalse();
    }

    @Test
    void aNewPaymentLinkCancelsTheOldOne() throws Exception {
        TestUser u = newUser();
        Long plan = newPlan(3, 14, "199");
        String first = json(subscribe(u, plan), "$.paymentId");
        String second = json(subscribe(u, plan), "$.paymentId");

        assertThat(paymentStatus(first)).isEqualTo("CANCELLED");
        payWithCard(first, TEST_CARD_OK);
        assertThat(paymentStatus(first)).as("paying a cancelled link does nothing").isEqualTo("CANCELLED");
        assertThat(paymentStatus(second)).isEqualTo("PENDING");
    }

    @Test
    void doubleClickChargesOnlyOnce() throws Exception {
        TestUser u = newUser();
        MvcResult r = subscribe(u, newPlan(3, 14, "199"));
        String paymentId = json(r, "$.paymentId");
        Long subscription = id(r, "$.referenceId");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> pay = () -> payWithCard(paymentId, TEST_CARD_OK);
            Future<Integer> a = pool.submit(pay);
            Future<Integer> b = pool.submit(pay);
            a.get();
            b.get();
        } finally {
            pool.shutdown();
        }

        assertThat(jdbc.queryForObject("select attempts from payments where payment_id = ?", Integer.class, paymentId))
                .as("charged once").isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from subscriptions where id = ?", String.class, subscription))
                .isEqualTo("ACTIVE");
    }

    @Test
    void cardNumbersAreNeverStored() throws Exception {
        TestUser u = newUser();
        payWithCard(json(subscribe(u, newPlan(3, 14, "199")), "$.paymentId"), TEST_CARD_OK);
        Integer found = jdbc.queryForObject("select count(*) from payments where concat_ws('|', payment_id, "
                + "description, provider_transaction_id, last_error, card_last4) like ?", Integer.class, "%" + TEST_CARD_OK + "%");
        assertThat(found).isZero();
    }

    @Test
    void freePlanActivatesWithoutPayment() throws Exception {
        TestUser u = newUser();
        MvcResult r = subscribe(u, newPlan(1, 7, "0"));
        assertThat((String) json(r, "$.status")).isEqualTo("SUCCESS");
        assertThat(canBorrow(u)).isTrue();
    }

    @Test
    void deskPaymentCancelsTheOpenOnlineLink() throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, newBook(1));
        String admin = adminToken();
        Long fine = id(call(post("/api/fines"), admin,
                "{\"bookLoanId\":%d,\"type\":\"DAMAGE\",\"amount\":5000}".formatted(loan)), "$.id");
        String link = json(call(post("/api/fines/" + fine + "/pay"), u.token(), null), "$.paymentId");

        assertThat(status(post("/api/fines/" + fine + "/record-payment?amount=2000"), admin, null)).isEqualTo(200);

        assertThat(paymentStatus(link)).isEqualTo("CANCELLED");
        MvcResult newLink = call(post("/api/fines/" + fine + "/pay"), u.token(), null);
        assertThat((Integer) json(newLink, "$.amount")).isEqualTo(3000);
    }

    @Test
    void checkoutPageEscapesHtml() throws Exception {
        TestUser u = newUser();
        Long plan = id(call(post("/api/subscription-plans"), adminToken(),
                "{\"title\":\"<script>alert(1)</script> %s\",\"price\":10,\"durationInDays\":30}"
                        .formatted(unique())), "$.id");
        String paymentId = json(subscribe(u, plan), "$.paymentId");

        String page = mvc.perform(get("/pay/" + paymentId)).andReturn().getResponse().getContentAsString();
        assertThat(page).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
    }

    @Test
    void checkoutPageLinksBackToTheFrontendResultPage() throws Exception {
        String paymentId = json(subscribe(newUser(), newPlan(3, 14, "50")), "$.paymentId");
        String link = "/payment/result?paymentId=" + paymentId;

        // Before paying: the form offers a way back.
        String form = mvc.perform(get("/pay/" + paymentId)).andReturn().getResponse().getContentAsString();
        assertThat(form).contains(link);

        // After paying: the result page links to the frontend's result page.
        assertThat(payWithCard(paymentId, "4242424242424242")).isEqualTo(200);
        String result = mvc.perform(get("/pay/" + paymentId)).andReturn().getResponse().getContentAsString();
        assertThat(result).contains("Payment successful").contains(link);
    }

    @Test
    void checkoutFormWorksWhenSubmittedFromABrowser() throws Exception {
        String paymentId = json(subscribe(newUser(), newPlan(3, 14, "50")), "$.paymentId");

        // "no-referrer" made browsers send "Origin: null" on the form post, which CORS refused.
        MvcResult page = mvc.perform(get("/pay/" + paymentId)).andReturn();
        assertThat(page.getResponse().getHeader("Referrer-Policy")).isEqualTo("same-origin");

        // A browser submitting the form sends the page's own origin: it must be accepted.
        int status = mvc.perform(post("/pay/" + paymentId)
                        .header("Origin", "http://localhost")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("cardNumber", "4242424242424242").param("expiry", "12/35").param("cvc", "123"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    void usersCannotSeeOthersPayments() throws Exception {
        String paymentId = json(subscribe(newUser(), newPlan(3, 14, "199")), "$.paymentId");
        assertThat(status(get("/api/payments/" + paymentId), newUser().token(), null)).isEqualTo(403);
    }
}
