package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Borrowing rules, copy counts, overdue fines and paying them. */
class LoanAndFineTest extends TestSupport {

    @Test
    void borrowingNeedsAnActiveSubscription() throws Exception {
        assertThat(status(post("/api/book-loans/checkout"), newUser().token(),
                "{\"bookId\":%d}".formatted(newBook(1)))).isEqualTo(400);
    }

    @Test
    void planBorrowLimitIsEnforced() throws Exception {
        TestUser u = newUser();
        subscribeAndPay(u, newPlan(1, 14, "50"));
        checkout(u, newBook(1));
        assertThat(status(post("/api/book-loans/checkout"), u.token(),
                "{\"bookId\":%d}".formatted(newBook(1)))).isEqualTo(400);
    }

    @Test
    void copyCountsFollowCheckoutAndReturn() throws Exception {
        TestUser u = subscribedUser();
        Long book = newBook(2);
        Long loan = checkout(u, book);
        assertThat(availableCopies(book)).isEqualTo(1);

        assertThat(status(post("/api/book-loans/checkout"), u.token(), "{\"bookId\":%d}".formatted(book)))
                .as("same book twice").isEqualTo(409);

        assertThat(status(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan)))
                .isEqualTo(200);
        assertThat(availableCopies(book)).isEqualTo(2);
        assertThat(status(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan)))
                .as("return twice").isEqualTo(409);
    }

    @Test
    void noCopiesLeftIsAConflict() throws Exception {
        Long book = newBook(1);
        checkout(subscribedUser(), book);
        assertThat(status(post("/api/book-loans/checkout"), subscribedUser().token(),
                "{\"bookId\":%d}".formatted(book))).isEqualTo(409);
    }

    @Test
    void overdueLoansCannotBeRenewed() throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, newBook(1));
        makeLate(loan, 20, 2);
        assertThat(status(post("/api/book-loans/renew"), u.token(), "{\"bookLoanId\":%d}".formatted(loan)))
                .isEqualTo(409);
    }

    @Test
    void lateReturnCreatesFiveMadPerDayFine() throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, newBook(1));
        makeLate(loan, 20, 4);
        call(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan));

        MvcResult fines = call(get("/api/fines/book-loan/" + loan), u.token(), null);
        assertThat((Integer) json(fines, "$[0].amount")).isEqualTo(2000);           // 4 days x 5.00 MAD
        assertThat((String) json(fines, "$[0].type")).isEqualTo("OVERDUE");
    }

    @Test
    void overdueFineIsCappedAtOneHundredMad() throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, newBook(1));
        makeLate(loan, 60, 40);
        call(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan));
        assertThat((Integer) json(call(get("/api/fines/book-loan/" + loan), u.token(), null), "$[0].amount"))
                .isEqualTo(10000);
    }

    @Test
    void unpaidFineBlocksBorrowingUntilPaid() throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, newBook(1));
        makeLate(loan, 20, 3);
        call(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan));
        Long book = newBook(1);
        assertThat(status(post("/api/book-loans/checkout"), u.token(), "{\"bookId\":%d}".formatted(book))).isEqualTo(400);

        Long fine = id(call(get("/api/fines/book-loan/" + loan), u.token(), null), "$[0].id");
        String paymentId = json(call(post("/api/fines/" + fine + "/pay"), u.token(), null), "$.paymentId");
        assertThat(payWithCard(paymentId, TEST_CARD_OK)).isEqualTo(200);

        assertThat((String) json(call(get("/api/fines/" + fine), u.token(), null), "$.status")).isEqualTo("PAID");
        checkout(u, book);
    }

    @Test
    void usersCannotSeeEachOthersLoansOrFines() throws Exception {
        TestUser owner = subscribedUser();
        Long loan = checkout(owner, newBook(1));
        TestUser other = newUser();
        assertThat(status(get("/api/book-loans/" + loan), other.token(), null)).isEqualTo(403);
        assertThat(status(get("/api/fines/book-loan/" + loan), other.token(), null)).isEqualTo(403);
        assertThat(status(get("/api/book-loans/" + loan), adminToken(), null)).isEqualTo(200);
    }
}
