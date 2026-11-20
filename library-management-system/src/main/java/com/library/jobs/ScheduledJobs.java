package com.library.jobs;

import com.library.repository.EmailVerificationTokenRepository;
import com.library.repository.PasswordResetTokenRepository;
import com.library.service.BookLoanService;
import com.library.service.PaymentService;
import com.library.service.ReservationService;
import com.library.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * Everything the library does on its own. Schedules are in application.properties (app.jobs.*),
 * using cron format "second minute hour day month weekday" in the server's time zone.
 * Admins can see the status and run a job by hand: GET/POST /api/admin/jobs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    public static final String OVERDUE_LOANS = "overdue-loans";
    public static final String EXPIRE_SUBSCRIPTIONS = "expire-subscriptions";
    public static final String EXPIRE_RESERVATIONS = "expire-reservations";
    public static final String EXPIRE_PAYMENTS = "expire-payments";
    public static final String DUE_REMINDERS = "due-reminders";
    public static final String CLEANUP_TOKENS = "cleanup-tokens";

    private final JobRunner runner;
    private final BookLoanService loanService;
    private final SubscriptionService subscriptionService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.jobs.enabled:true}")
    private boolean enabled;

    @Value("${app.jobs.due-reminder-days-before:2}")
    private int reminderDaysBefore;

    @Value("${app.jobs.overdue-loans.cron}")
    private String overdueCron;
    @Value("${app.jobs.expire-subscriptions.cron}")
    private String subscriptionsCron;
    @Value("${app.jobs.expire-reservations.cron}")
    private String reservationsCron;
    @Value("${app.jobs.expire-payments.cron}")
    private String paymentsCron;
    @Value("${app.jobs.due-reminders.cron}")
    private String remindersCron;
    @Value("${app.jobs.cleanup-tokens.cron}")
    private String cleanupCron;

    @PostConstruct
    void registerJobs() {
        runner.register(OVERDUE_LOANS, "Mark late loans OVERDUE and update late days", overdueCron,
                loanService::updateOverdueLoans);
        runner.register(EXPIRE_SUBSCRIPTIONS, "Mark ended subscriptions EXPIRED", subscriptionsCron,
                subscriptionService::deactivateExpired);
        runner.register(EXPIRE_RESERVATIONS, "Expire held copies not picked up in time; pass them on", reservationsCron,
                reservationService::expireUncollected);
        runner.register(EXPIRE_PAYMENTS, "Expire unpaid payment links", paymentsCron,
                paymentService::expireStale);
        runner.register(DUE_REMINDERS, "Email 'book due soon' reminders (" + reminderDaysBefore + " days before)",
                remindersCron, () -> loanService.sendDueReminders(reminderDaysBefore));
        runner.register(CLEANUP_TOKENS, "Delete expired password-reset and email-verification tokens", cleanupCron,
                () -> transactionTemplate.execute(s -> resetTokens.deleteExpired(LocalDateTime.now())
                        + verificationTokens.deleteExpired(LocalDateTime.now())));
    }

    /**
     * Catch-up at startup: if the server was off when a job should have run (e.g. at midnight),
     * do the expiry work now. Reminders are not sent here; they keep their normal daily time.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        if (!enabled) {
            log.info("Background jobs are disabled (app.jobs.enabled=false)");
            return;
        }
        for (String job : new String[]{OVERDUE_LOANS, EXPIRE_SUBSCRIPTIONS, EXPIRE_RESERVATIONS, EXPIRE_PAYMENTS}) {
            runner.run(job, "startup");
        }
        log.info("Background jobs scheduled: {}", runner.statuses().stream().map(JobStatus::name).toList());
    }

    // ------------------------------------------------------------------ the schedules

    @Scheduled(cron = "${app.jobs.overdue-loans.cron}")
    public void overdueLoans() {
        if (enabled) runner.run(OVERDUE_LOANS, "scheduled");
    }

    @Scheduled(cron = "${app.jobs.expire-subscriptions.cron}")
    public void expireSubscriptions() {
        if (enabled) runner.run(EXPIRE_SUBSCRIPTIONS, "scheduled");
    }

    @Scheduled(cron = "${app.jobs.expire-reservations.cron}")
    public void expireReservations() {
        if (enabled) runner.run(EXPIRE_RESERVATIONS, "scheduled");
    }

    @Scheduled(cron = "${app.jobs.expire-payments.cron}")
    public void expirePayments() {
        if (enabled) runner.run(EXPIRE_PAYMENTS, "scheduled");
    }

    @Scheduled(cron = "${app.jobs.due-reminders.cron}")
    public void dueReminders() {
        if (enabled) runner.run(DUE_REMINDERS, "scheduled");
    }

    @Scheduled(cron = "${app.jobs.cleanup-tokens.cron}")
    public void cleanupTokens() {
        if (enabled) runner.run(CLEANUP_TOKENS, "scheduled");
    }
}
