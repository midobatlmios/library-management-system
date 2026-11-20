package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** The admin dashboard's daily activity (charts). */
class AdminStatisticsTest extends TestSupport {

    @Test
    void dailyActivityCountsTodaysCheckoutAndPayment() throws Exception {
        // subscribedUser() pays 100 MAD today; then borrows one book today.
        TestUser member = subscribedUser();
        checkout(member, newBook(1));

        MvcResult r = call(get("/api/admin/statistics/daily?days=7"), adminToken(), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((List<?>) json(r, "$.days")).hasSize(7);
        assertThat((String) json(r, "$.to")).isEqualTo(LocalDate.now().toString());
        assertThat((String) json(r, "$.days[6].date")).isEqualTo(LocalDate.now().toString());
        assertThat(((Number) json(r, "$.days[6].checkouts")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) json(r, "$.days[6].collected")).doubleValue()).isGreaterThanOrEqualTo(100.0);
        assertThat((String) json(r, "$.currency")).isEqualTo("MAD");
    }

    @Test
    void dayCountIsClampedAndAdminOnly() throws Exception {
        String admin = adminToken();
        assertThat((List<?>) json(call(get("/api/admin/statistics/daily?days=0"), admin, null), "$.days")).hasSize(1);
        assertThat((List<?>) json(call(get("/api/admin/statistics/daily?days=5000"), admin, null), "$.days")).hasSize(365);
        assertThat(status(get("/api/admin/statistics/daily"), newUser().token(), null)).isEqualTo(403);
        assertThat(status(get("/api/admin/statistics/daily"), null, null)).isEqualTo(401);
    }
}
