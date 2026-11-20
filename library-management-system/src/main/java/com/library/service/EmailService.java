package com.library.service;

import com.library.model.BookLoan;
import com.library.model.Payment;
import com.library.model.Reservation;
import com.library.model.User;

/**
 * Every email the library sends. Emails are built immediately but only SENT after the current
 * database transaction commits (so no email goes out for something that was rolled back), and in the
 * background (so the user never waits for the mail server). A sending failure is logged, never thrown.
 */
public interface EmailService {

    void sendEmailVerification(User user, String verifyLink);

    void sendPasswordReset(User user, String resetLink);

    void sendReservationReady(Reservation reservation);

    void sendPaymentReceipt(Payment payment);

    /** Used by the scheduled jobs (step 13). */
    void sendDueSoonReminder(BookLoan loan);

    /** Sends at once and waits; for the admin "test email" endpoint. Returns null on success, else the error. */
    String sendTestNow(String to);
}
