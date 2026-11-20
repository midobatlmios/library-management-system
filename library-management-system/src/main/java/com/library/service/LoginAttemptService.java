package com.library.service;

import com.library.exception.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory brute-force protection (per app instance; enough for one server).
 *  - Login: max N failures per email+IP, and 4N per IP (password spraying), within the window.
 *  - Forgot password: max M requests per email+IP within the window.
 * Answers 429 Too Many Requests when a limit is hit.
 */
@Service
public class LoginAttemptService {

    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginFailures;

    @Value("${app.security.max-reset-requests:3}")
    private int maxResetRequests;

    @Value("${app.security.attempt-window-minutes:15}")
    private int windowMinutes;

    public void checkLoginAllowed(String email, String ip) throws UserException {
        check("login:" + key(email, ip), maxLoginFailures);
        check("login-ip:" + ip, maxLoginFailures * 4);
    }

    public void loginFailed(String email, String ip) {
        record("login:" + key(email, ip));
        record("login-ip:" + ip);
    }

    public void loginSucceeded(String email, String ip) {
        events.remove("login:" + key(email, ip));
    }

    /** Checks and counts one forgot-password request. */
    public void checkResetRequest(String email, String ip) throws UserException {
        String k = "reset:" + key(email, ip);
        check(k, maxResetRequests);
        record(k);
    }

    // ------------------------------------------------------------------ internals

    private void check(String k, int max) throws UserException {
        Deque<Instant> times = recent(k);
        if (times.size() >= max) {
            long minutesLeft = Math.max(1, Duration.between(Instant.now(),
                    times.peekFirst().plus(Duration.ofMinutes(windowMinutes))).toMinutes() + 1);
            throw new UserException("Too many attempts. Please try again in " + minutesLeft + " minute(s).",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private void record(String k) {
        events.computeIfAbsent(k, x -> new ConcurrentLinkedDeque<>()).addLast(Instant.now());
        if (events.size() > 10_000) {
            cleanup();
        }
    }

    /** Drops events older than the window and returns what is left for this key. */
    private Deque<Instant> recent(String k) {
        Deque<Instant> times = events.getOrDefault(k, new ConcurrentLinkedDeque<>());
        Instant limit = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        while (!times.isEmpty() && times.peekFirst().isBefore(limit)) {
            times.pollFirst();
        }
        return times;
    }

    private void cleanup() {
        events.keySet().removeIf(k -> recent(k).isEmpty());
    }

    private static String key(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "|" + ip;
    }
}
