package com.library.service.impl;

import com.library.model.BookLoan;
import com.library.model.Payment;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.service.EmailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final TaskExecutor taskExecutor;

    /** Emails go on the normal background pool, not the scheduler's threads (both are TaskExecutors). */
    public SmtpEmailService(JavaMailSender mailSender,
                            @org.springframework.beans.factory.annotation.Qualifier("applicationTaskExecutor")
                            TaskExecutor taskExecutor) {
        this.mailSender = mailSender;
        this.taskExecutor = taskExecutor;
    }

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.mail.from-name:Library}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Domains that are never emailed (reserved test domains). */
    @Value("#{'${app.mail.skip-domains:example.com,example.org,example.net}'.toLowerCase().split(',')}")
    private java.util.List<String> skipDomains;

    // ------------------------------------------------------------------ the emails

    @Override
    public void sendEmailVerification(User user, String verifyLink) {
        queue(user.getEmail(), "Confirm your email address", layout("Welcome, " + user.getFullName() + "!",
                p("Please confirm your email address to finish setting up your library account.")
                        + button("Confirm my email", verifyLink)
                        + small("This link is valid for 24 hours. If you did not create an account, ignore this email.")));
    }

    @Override
    public void sendPasswordReset(User user, String resetLink) {
        queue(user.getEmail(), "Reset your password", layout("Reset your password",
                p("Hello " + user.getFullName() + ", we received a request to reset your password.")
                        + button("Choose a new password", resetLink)
                        + small("This link is valid for a short time and works once. "
                        + "If you did not ask for this, you can ignore this email: your password stays the same.")));
    }

    @Override
    public void sendReservationReady(Reservation r) {
        queue(r.getUser().getEmail(), "Your reserved book is ready: " + r.getBook().getTitle(),
                layout("Your book is ready for pickup",
                        pRaw("Good news, " + esc(r.getUser().getFullName()) + "! A copy of <b>" + esc(r.getBook().getTitle())
                                + "</b> is set aside for you.")
                                + p("Please pick it up before " + r.getAvailableUntil().format(DATE_TIME)
                                + ". After that, it goes to the next person in line.")
                                + button("View my reservations", frontendUrl + "/reservations")));
    }

    @Override
    public void sendPaymentReceipt(Payment pay) {
        queue(pay.getUser().getEmail(), "Payment receipt - " + pay.getAmountInMajorUnits() + " " + pay.getCurrency(),
                layout("Thank you for your payment",
                        table(new String[][]{
                                {"Description", pay.getDescription()},
                                {"Amount", pay.getAmountInMajorUnits() + " " + pay.getCurrency()},
                                {"Card", "**** " + pay.getCardLast4()},
                                {"Reference", pay.getProviderTransactionId()},
                                {"Payment id", pay.getPaymentId()},
                                {"Date", pay.getCompletedAt().format(DATE_TIME)}})
                                + small("Simulated payment: no real money was charged.")));
    }

    @Override
    public void sendDueSoonReminder(BookLoan loan) {
        queue(loan.getUser().getEmail(), "Reminder: \"" + loan.getBook().getTitle() + "\" is due " + loan.getDueDate().format(DATE),
                layout("Your book is due soon",
                        pRaw("Hello " + esc(loan.getUser().getFullName()) + ", please return or renew <b>"
                                + esc(loan.getBook().getTitle()) + "</b> by " + loan.getDueDate().format(DATE) + ".")
                                + p("Late returns are charged a fine for each day.")
                                + button("View my loans", frontendUrl + "/loans")));
    }

    @Override
    public String sendTestNow(String to) {
        if (!isConfigured()) {
            return "Email is not configured (spring.mail.username / spring.mail.password)";
        }
        try {
            deliver(to, "Library test email", layout("It works!", p("Your library backend can send emails.")));
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------ sending

    private boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank();
    }

    /** Sends after the current transaction commits (or now if there is none), in the background. */
    private void queue(String to, String subject, String html) {
        if (!isConfigured()) {
            log.warn("Email not configured; not sending \"{}\"", subject);
            return;
        }
        String domain = to.substring(to.lastIndexOf('@') + 1).toLowerCase();
        if (skipDomains.contains(domain)) {
            // Reserved test domains can never receive mail; sending would only bounce back to our inbox.
            log.info("[mail skipped: test domain {}] \"{}\"", domain, subject);
            return;
        }
        Runnable send = () -> taskExecutor.execute(() -> {
            try {
                deliver(to, subject, html);
                log.info("Email \"{}\" sent", subject);
            } catch (Exception e) {
                log.error("Could not send email \"{}\": {}", subject, e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void deliver(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    // ------------------------------------------------------------------ tiny HTML helpers (all text escaped)

    private static String layout(String title, String body) {
        return """
                <div style="font-family:Arial,sans-serif;background:#f4f5f7;padding:24px">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:10px;padding:24px;color:#1d2129">
                    <h2 style="margin-top:0">%s</h2>%s
                    <hr style="border:0;border-top:1px solid #eee;margin:24px 0 12px">
                    <p style="color:#888;font-size:12px;margin:0">Library Management System</p>
                  </div>
                </div>""".formatted(esc(title), body);
    }

    private static String p(String text) {
        return pRaw(esc(text));
    }

    /** For HTML the caller built itself: every user value inside MUST already be passed through esc(). */
    private static String pRaw(String html) {
        return "<p style=\"line-height:1.5\">" + html + "</p>";
    }

    private static String small(String text) {
        return "<p style=\"color:#666;font-size:13px;line-height:1.5\">" + esc(text) + "</p>";
    }

    private static String button(String label, String url) {
        return "<p style=\"margin:24px 0\"><a href=\"" + esc(url) + "\" style=\"background:#1a5fd0;color:#fff;"
                + "padding:12px 20px;border-radius:8px;text-decoration:none;display:inline-block\">" + esc(label) + "</a></p>"
                + "<p style=\"color:#666;font-size:12px;word-break:break-all\">Or open: " + esc(url) + "</p>";
    }

    private static String table(String[][] rows) {
        StringBuilder sb = new StringBuilder("<table style=\"width:100%;border-collapse:collapse\">");
        for (String[] r : rows) {
            sb.append("<tr><td style=\"padding:6px 0;color:#666\">").append(esc(r[0]))
                    .append("</td><td style=\"padding:6px 0;text-align:right\">").append(esc(r[1])).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
