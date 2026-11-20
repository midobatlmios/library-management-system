package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Background jobs, run by hand through the admin API (the schedules are off during tests). */
class JobsTest extends TestSupport {

    private MvcResult run(String job) throws Exception {
        MvcResult r = call(post("/api/admin/jobs/" + job + "/run"), adminToken(), null);
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(200);
        assertThat((Object) json(r, "$.lastError")).isNull();
        return r;
    }

    @Test
    void overdueJobMarksLateLoans() throws Exception {
        Long loan = checkout(subscribedUser(), newBook(1));
        makeLate(loan, 20, 3);
        run("overdue-loans");
        assertThat(jdbc.queryForObject("select status from book_loans where id = ?", String.class, loan))
                .isEqualTo("OVERDUE");
        assertThat(jdbc.queryForObject("select overdue_days from book_loans where id = ?", Integer.class, loan))
                .isEqualTo(3);
    }

    @Test
    void dueSoonReminderIsSentOncePerDueDate() throws Exception {
        Long loan = checkout(subscribedUser(), newBook(1));
        jdbc.update("update book_loans set due_date = date_add(curdate(), interval 2 day) where id = ?", loan);

        run("due-reminders");
        verify(emailService, atLeastOnce()).sendDueSoonReminder(any());
        assertThat(jdbc.queryForObject("select due_reminder_sent_for is not null from book_loans where id = ?",
                Boolean.class, loan)).isTrue();

        MvcResult again = run("due-reminders");
        assertThat(jdbc.queryForObject("select count(*) from book_loans where id = ? and due_reminder_sent_for = due_date",
                Integer.class, loan)).isEqualTo(1);
        assertThat((Integer) json(again, "$.lastResultCount")).as("no second reminder").isZero();
    }

    @Test
    void expiredSubscriptionsAreClosed() throws Exception {
        TestUser u = newUser();
        Long sub = subscribeAndPay(u, newPlan(3, 14, "100"));
        jdbc.update("update subscriptions set start_date = date_sub(curdate(), interval 40 day), "
                + "end_date = date_sub(curdate(), interval 1 day) where id = ?", sub);
        run("expire-subscriptions");
        assertThat(jdbc.queryForObject("select status from subscriptions where id = ?", String.class, sub))
                .isEqualTo("EXPIRED");
    }

    @Test
    void unpaidPaymentLinksExpire() throws Exception {
        TestUser u = newUser();
        String paymentId = json(call(post("/api/subscriptions/subscribe"), u.token(),
                "{\"planId\":%d}".formatted(newPlan(3, 14, "100"))), "$.paymentId");
        jdbc.update("update payments set expires_at = date_sub(now(), interval 1 minute) where payment_id = ?", paymentId);
        run("expire-payments");
        assertThat(jdbc.queryForObject("select status from payments where payment_id = ?", String.class, paymentId))
                .isEqualTo("EXPIRED");
    }

    @Test
    void unknownJobIs404() throws Exception {
        assertThat(status(post("/api/admin/jobs/nope/run"), adminToken(), null)).isEqualTo(404);
    }
}
