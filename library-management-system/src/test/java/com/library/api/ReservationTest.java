package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** The waiting queue: held copies, fulfilment, cancel and expiry. */
class ReservationTest extends TestSupport {

    private Long reserve(TestUser u, Long book) throws Exception {
        MvcResult r = call(post("/api/reservations"), u.token(), "{\"bookId\":%d}".formatted(book));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    private String statusOf(Long reservation) throws Exception {
        return json(call(get("/api/reservations/" + reservation), adminToken(), null), "$.status");
    }

    private Integer positionOf(Long reservation) throws Exception {
        return json(call(get("/api/reservations/" + reservation), adminToken(), null), "$.queuePosition");
    }

    @Test
    void cannotReserveABookThatIsOnTheShelf() throws Exception {
        assertThat(status(post("/api/reservations"), subscribedUser().token(),
                "{\"bookId\":%d}".formatted(newBook(2)))).isEqualTo(400);
    }

    @Test
    void returnedCopyIsHeldForTheFirstPersonInLine() throws Exception {
        Long book = newBook(1);
        TestUser alice = subscribedUser(), bob = subscribedUser(), carol = subscribedUser();
        Long aliceLoan = checkout(alice, book);
        Long bobRes = reserve(bob, book);
        Long carolRes = reserve(carol, book);
        assertThat(positionOf(bobRes)).isEqualTo(1);
        assertThat(positionOf(carolRes)).isEqualTo(2);

        assertThat(status(post("/api/book-loans/renew"), alice.token(), "{\"bookLoanId\":%d}".formatted(aliceLoan)))
                .as("no renewal while others wait").isEqualTo(409);

        call(post("/api/book-loans/check-in"), alice.token(), "{\"bookLoanId\":%d}".formatted(aliceLoan));

        assertThat(statusOf(bobRes)).isEqualTo("AVAILABLE");
        assertThat(availableCopies(book)).as("held, not on the shelf").isZero();
        assertThat(positionOf(carolRes)).isEqualTo(1);
        verify(emailService).sendReservationReady(argThat(r -> r.getId().equals(bobRes)));

        assertThat(status(post("/api/book-loans/checkout"), carol.token(), "{\"bookId\":%d}".formatted(book)))
                .as("carol can't take bob's copy").isEqualTo(409);
        assertThat(status(post("/api/reservations/" + carolRes + "/fulfill"), carol.token(), null))
                .as("not carol's turn yet").isEqualTo(409);

        MvcResult fulfilled = call(post("/api/reservations/" + bobRes + "/fulfill"), bob.token(), null);
        assertThat((String) json(fulfilled, "$.status")).isEqualTo("FULFILLED");
        Long loan = id(fulfilled, "$.fulfilledLoanId");
        assertThat((String) json(call(get("/api/book-loans/" + loan), bob.token(), null), "$.status"))
                .isEqualTo("CHECKED_OUT");
    }

    @Test
    void cancellingAHeldCopyPassesItToTheNextPerson() throws Exception {
        Long book = newBook(1);
        TestUser alice = subscribedUser(), bob = subscribedUser(), carol = subscribedUser();
        Long loan = checkout(alice, book);
        Long bobRes = reserve(bob, book);
        Long carolRes = reserve(carol, book);
        call(post("/api/book-loans/check-in"), alice.token(), "{\"bookLoanId\":%d}".formatted(loan));

        assertThat(status(delete("/api/reservations/" + bobRes), bob.token(), null)).isEqualTo(200);
        assertThat(statusOf(carolRes)).isEqualTo("AVAILABLE");
        assertThat(availableCopies(book)).isZero();
    }

    @Test
    void uncollectedCopyExpiresAndGoesBackToTheShelf() throws Exception {
        Long book = newBook(1);
        TestUser alice = subscribedUser(), bob = subscribedUser();
        Long loan = checkout(alice, book);
        Long bobRes = reserve(bob, book);
        call(post("/api/book-loans/check-in"), alice.token(), "{\"bookLoanId\":%d}".formatted(loan));
        jdbc.update("update reservations set available_until = date_sub(now(), interval 1 hour) where id = ?", bobRes);

        assertThat(status(post("/api/admin/jobs/expire-reservations/run"), adminToken(), null)).isEqualTo(200);

        assertThat(statusOf(bobRes)).isEqualTo("EXPIRED");
        assertThat(availableCopies(book)).isEqualTo(1);
    }

    @Test
    void usersCannotReadOthersReservations() throws Exception {
        Long book = newBook(1);
        checkout(subscribedUser(), book);
        Long res = reserve(subscribedUser(), book);
        assertThat(status(get("/api/reservations/" + res), newUser().token(), null)).isEqualTo(403);
    }
}
