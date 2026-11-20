package com.library.service.impl;

import com.library.domain.PaymentReferenceType;
import com.library.domain.PaymentStatus;
import com.library.exception.LibraryException;
import com.library.exception.PaymentException;
import com.library.model.Payment;
import com.library.model.User;
import com.library.payload.dto.PaymentDTO;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.payment.ChargeResult;
import com.library.payment.PaymentTargetHandler;
import com.library.payment.SimulatedPaymentGateway;
import com.library.repository.PaymentRepository;
import com.library.service.PaymentService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class PaymentServiceImpl implements PaymentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRepository paymentRepository;
    private final SimulatedPaymentGateway gateway;
    private final UserService userService;
    private final com.library.service.EmailService emailService;
    /** Looked up lazily: Subscription and Fine services implement it and also use this service. */
    private final ObjectProvider<PaymentTargetHandler> handlers;

    @Value("${app.payment.expiry-minutes:30}")
    private long expiryMinutes;

    @Value("${app.payment.max-attempts:3}")
    private int maxAttempts;

    // ------------------------------------------------------------------ create / cancel

    @Override
    public PaymentInitiateResponse create(User payer, PaymentReferenceType type, Long referenceId,
                                          long amount, String currency, String description) {
        cancelPendingFor(type, referenceId, "Replaced by a newer payment");

        Payment p = Payment.builder()
                .paymentId("PAY_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT))
                .user(payer)
                .referenceType(type)
                .referenceId(referenceId)
                .description(description.length() > 255 ? description.substring(0, 255) : description)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .provider(gateway.providerName())
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        paymentRepository.save(p);

        return PaymentInitiateResponse.builder()
                .paymentId(p.getPaymentId())
                .referenceType(type.name())
                .referenceId(referenceId)
                .description(p.getDescription())
                .amount(amount)
                .amountInMajorUnits(BigDecimal.valueOf(amount, 2))
                .currency(currency)
                .status(PaymentStatus.PENDING.name())
                .checkoutUrl(gateway.checkoutUrl(p))
                .expiresAt(p.getExpiresAt())
                .message("Open checkoutUrl to complete the payment before it expires.")
                .build();
    }

    @Override
    public void cancelPendingFor(PaymentReferenceType type, Long referenceId, String reason) {
        for (Payment old : paymentRepository.findByReferenceTypeAndReferenceIdAndStatus(
                type, referenceId, PaymentStatus.PENDING)) {
            old.setStatus(PaymentStatus.CANCELLED);
            old.setLastError(reason);
            old.setCompletedAt(LocalDateTime.now());
        }
    }

    // ------------------------------------------------------------------ checkout

    @Override
    public Payment getForCheckout(String paymentId) throws LibraryException {
        Payment p = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found", HttpStatus.NOT_FOUND));
        if (p.isExpiredNow()) {
            expire(p);
        }
        p.getUser().getId();   // load what the page needs while the transaction is open
        return p;
    }

    @Override
    public CheckoutOutcome payWithCard(String paymentId, String cardNumber, String expiry, String cvc)
            throws LibraryException {
        // Row lock: a double click or two tabs can't pay the same payment twice.
        Payment p = paymentRepository.lockByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found", HttpStatus.NOT_FOUND));

        if (p.isExpiredNow()) {
            expire(p);
        }
        if (p.getStatus() != PaymentStatus.PENDING) {
            return new CheckoutOutcome(p, null, "This payment is " + p.getStatus().name().toLowerCase(Locale.ROOT));
        }

        PaymentTargetHandler handler = handlerFor(p.getReferenceType());
        try {
            handler.checkStillPayable(p);
        } catch (LibraryException e) {
            p.setStatus(PaymentStatus.CANCELLED);
            p.setLastError(e.getMessage());
            p.setCompletedAt(LocalDateTime.now());
            return new CheckoutOutcome(p, null, "This payment is no longer needed: " + e.getMessage());
        }

        ChargeResult result = gateway.charge(cardNumber, expiry, cvc);
        switch (result.outcome()) {
            case INVALID -> {
                return new CheckoutOutcome(p, result.outcome(), result.message());
            }
            case DECLINED -> {
                p.setAttempts(p.getAttempts() + 1);
                p.setLastError(result.message());
                if (p.getAttempts() >= maxAttempts) {
                    p.setStatus(PaymentStatus.FAILED);
                    p.setCompletedAt(LocalDateTime.now());
                    return new CheckoutOutcome(p, result.outcome(), result.message()
                            + ". Too many declined attempts: this payment is closed, please start a new one.");
                }
                return new CheckoutOutcome(p, result.outcome(), result.message() + ". Attempt "
                        + p.getAttempts() + " of " + maxAttempts + ".");
            }
            default -> {
                p.setAttempts(p.getAttempts() + 1);
                p.setStatus(PaymentStatus.SUCCESS);
                p.setProviderTransactionId(result.transactionId());
                p.setCardLast4(result.cardLast4());
                p.setLastError(null);
                p.setCompletedAt(LocalDateTime.now());
                // Activate the subscription / clear the fine. If this fails, everything rolls back.
                handler.onPaymentSucceeded(p);
                emailService.sendPaymentReceipt(p);       // sent only if this transaction commits
                log.info("Payment {} succeeded ({} {} for {} #{})", p.getPaymentId(),
                        p.getAmountInMajorUnits(), p.getCurrency(), p.getReferenceType(), p.getReferenceId());
                return new CheckoutOutcome(p, result.outcome(), "Payment successful");
            }
        }
    }

    // ------------------------------------------------------------------ API

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getById(String paymentId) throws LibraryException {
        return PaymentDTO.from(findOwned(paymentId));
    }

    @Override
    public PaymentDTO cancel(String paymentId) throws LibraryException {
        Payment p = findOwned(paymentId);
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Payment is already " + p.getStatus(), HttpStatus.CONFLICT);
        }
        p.setStatus(PaymentStatus.CANCELLED);
        p.setLastError("Cancelled by user");
        p.setCompletedAt(LocalDateTime.now());
        return PaymentDTO.from(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<PaymentDTO> getMyPayments(int page, int size) throws LibraryException {
        checkPage(page, size);
        return toPage(paymentRepository.findByUser(userService.getCurrentUser(),
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()))));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<PaymentDTO> search(Long userId, String status, String type, int page, int size)
            throws LibraryException {
        checkPage(page, size);
        return toPage(paymentRepository.search(userId,
                parse(PaymentStatus.class, status, "status"),
                parse(PaymentReferenceType.class, type, "type"),
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()))));
    }

    @Override
    public int expireStale() {
        List<Payment> stale = paymentRepository.findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, LocalDateTime.now());
        stale.forEach(this::expire);
        return stale.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long subs = paymentRepository.sumSucceeded(PaymentReferenceType.SUBSCRIPTION);
        long fines = paymentRepository.sumSucceeded(PaymentReferenceType.FINE);
        stats.put("collectedSubscriptions", BigDecimal.valueOf(subs, 2));
        stats.put("collectedFines", BigDecimal.valueOf(fines, 2));
        stats.put("collectedTotal", BigDecimal.valueOf(subs + fines, 2));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PaymentStatus s : PaymentStatus.values()) {
            byStatus.put(s.name(), paymentRepository.countByStatus(s));
        }
        stats.put("paymentsByStatus", byStatus);
        return stats;
    }

    // ------------------------------------------------------------------ helpers

    private void expire(Payment p) {
        p.setStatus(PaymentStatus.EXPIRED);
        p.setLastError("Not paid before " + p.getExpiresAt());
        p.setCompletedAt(LocalDateTime.now());
    }

    private PaymentTargetHandler handlerFor(PaymentReferenceType type) throws PaymentException {
        return handlers.orderedStream()
                .filter(h -> h.type() == type)
                .findFirst()
                .orElseThrow(() -> new PaymentException("No handler for " + type, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Payment findOwned(String paymentId) throws LibraryException {
        Payment p = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found", HttpStatus.NOT_FOUND));
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !p.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return p;
    }

    private static void checkPage(int page, int size) throws PaymentException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new PaymentException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private static PagesResponse<PaymentDTO> toPage(Page<Payment> p) {
        return new PagesResponse<>(PaymentDTO.fromList(p.getContent()),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                p.isLast(), p.isFirst(), p.isEmpty());
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String name) throws PaymentException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PaymentException("Unknown " + name + " '" + value + "'. Allowed: "
                    + java.util.Arrays.toString(type.getEnumConstants()));
        }
    }
}
