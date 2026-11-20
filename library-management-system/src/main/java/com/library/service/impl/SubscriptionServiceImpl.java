package com.library.service.impl;

import com.library.domain.SubscriptionStatus;
import com.library.exception.LibraryException;
import com.library.exception.SubscriptionException;
import com.library.exception.SubscriptionPlanException;
import com.library.exception.UserException;
import com.library.mapper.SubscriptionMapper;
import com.library.model.Subscription;
import com.library.model.SubscriptionPlan;
import com.library.model.User;
import com.library.payload.dto.SubscriptionDTO;
import com.library.payload.request.SubscribeRequest;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.repository.SubscriptionPlanRepository;
import com.library.repository.SubscriptionRepository;
import com.library.repository.UserRepository;
import com.library.service.SubscriptionService;
import com.library.service.UserService;
import com.library.service.PaymentService;
import com.library.domain.PaymentReferenceType;
import com.library.model.Payment;
import com.library.payment.PaymentTargetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class SubscriptionServiceImpl implements SubscriptionService, PaymentTargetHandler {

    private static final int MAX_PAGE_SIZE = 100;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PaymentService paymentService;

    @org.springframework.beans.factory.annotation.Value("${app.security.require-verified-email:true}")
    private boolean requireVerifiedEmail;

    // ------------------------------------------------------------------ subscribe / pay

    @Override
    public PaymentInitiateResponse subscribe(SubscribeRequest request) throws LibraryException {
        User user = userService.getCurrentUser();
        if (requireVerifiedEmail && !Boolean.TRUE.equals(user.getVerified())) {
            throw new SubscriptionException("Please confirm your email address before subscribing. "
                    + "Check your inbox, or ask for a new link with POST /auth/resend-verification.",
                    HttpStatus.FORBIDDEN);
        }

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .filter(SubscriptionPlan::getActive)
                .orElseThrow(() -> new SubscriptionPlanException(
                        "Subscription plan not found with id: " + request.getPlanId(), HttpStatus.NOT_FOUND));

        Optional<Subscription> current = findValidSubscription(user);
        if (current.isPresent()) {
            throw new SubscriptionException("You already have an active subscription (id "
                    + current.get().getId() + "). Use renew to extend it.", HttpStatus.CONFLICT);
        }

        cancelUnpaidSubscriptions(user);

        // Trial days only for a user's very first paid subscription.
        int trialDays = subscriptionRepository.existsByUserAndActivatedAtIsNotNull(user) ? 0 : plan.getTrialDays();

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.initializeFromPlan(plan, trialDays);
        sub.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : plan.getAutoRenew());
        sub.setNotes(request.getNotes());
        sub.setStatus(SubscriptionStatus.PENDING);

        return startPayment(subscriptionRepository.save(sub));
    }

    // ------------------------------------------------------------------ payment hooks (PaymentTargetHandler)

    @Override
    public PaymentReferenceType type() {
        return PaymentReferenceType.SUBSCRIPTION;
    }

    @Override
    public void checkStillPayable(Payment payment) throws LibraryException {
        Subscription sub = subscriptionRepository.findById(payment.getReferenceId())
                .orElseThrow(() -> new SubscriptionException("Subscription no longer exists", HttpStatus.NOT_FOUND));
        if (sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new SubscriptionException("subscription is already " + sub.getStatus(), HttpStatus.CONFLICT);
        }
        if (!payment.getPaymentId().equals(sub.getPaymentId())) {
            throw new SubscriptionException("a newer payment was started for this subscription", HttpStatus.CONFLICT);
        }
    }

    /** Only the payment system calls this, after a successful charge. Users can no longer activate themselves. */
    @Override
    public void onPaymentSucceeded(Payment payment) throws LibraryException {
        checkStillPayable(payment);
        activateNow(subscriptionRepository.findById(payment.getReferenceId()).orElseThrow());
    }

    private Subscription activateNow(Subscription sub) {
        // A renewal paid early starts the day after the current subscription ends.
        // A brand-new subscription always starts today.
        LocalDate today = LocalDate.now();
        LocalDate start = today;
        if (sub.getRenewedFromId() != null) {
            start = subscriptionRepository.findLatestActiveEndDate(sub.getUser(), today)
                    .map(end -> end.plusDays(1))
                    .orElse(today);
        }

        sub.setStartDate(start);
        sub.calculateEndDate();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setActivatedAt(LocalDateTime.now());

        return subscriptionRepository.save(sub);
    }

    /** Free plans are activated at once; paid ones get a payment and a checkout link. */
    private PaymentInitiateResponse startPayment(Subscription sub) {
        if (sub.getPrice() == 0) {
            activateNow(sub);
            return PaymentInitiateResponse.builder()
                    .referenceType(PaymentReferenceType.SUBSCRIPTION.name())
                    .referenceId(sub.getId())
                    .description("Subscription: " + sub.getPlanName())
                    .amount(0L)
                    .amountInMajorUnits(sub.getPriceInMajorUnits())
                    .currency(sub.getCurrency())
                    .status("SUCCESS")
                    .message("Free plan: your subscription is active.")
                    .build();
        }
        PaymentInitiateResponse payment = paymentService.create(sub.getUser(), PaymentReferenceType.SUBSCRIPTION,
                sub.getId(), sub.getPrice(), sub.getCurrency(),
                "Subscription: " + sub.getPlanName() + (sub.getRenewedFromId() != null ? " (renewal)" : ""));
        sub.setPaymentId(payment.getPaymentId());
        subscriptionRepository.save(sub);
        return payment;
    }

    @Override
    public PaymentInitiateResponse renew(Long subscriptionId) throws LibraryException {
        Subscription old = findOwned(subscriptionId);

        if (old.getStatus() != SubscriptionStatus.ACTIVE && old.getStatus() != SubscriptionStatus.EXPIRED) {
            throw new SubscriptionException("Only active or expired subscriptions can be renewed; this one is "
                    + old.getStatus(), HttpStatus.CONFLICT);
        }
        SubscriptionPlan plan = old.getPlan();
        if (!plan.getActive()) {
            throw new SubscriptionException("Plan '" + plan.getTitle()
                    + "' is no longer available. Please subscribe to another plan.");
        }
        User user = old.getUser();
        if (subscriptionRepository.hasQueuedRenewal(user, LocalDate.now())) {
            throw new SubscriptionException("A renewal is already paid and waiting to start", HttpStatus.CONFLICT);
        }

        cancelUnpaidSubscriptions(user);

        // Renewals use the plan's current price and limits, with no trial.
        Subscription renewal = new Subscription();
        renewal.setUser(user);
        renewal.initializeFromPlan(plan, 0);
        renewal.setAutoRenew(old.getAutoRenew());
        renewal.setRenewedFromId(old.getId());
        renewal.setStatus(SubscriptionStatus.PENDING);

        return startPayment(subscriptionRepository.save(renewal));
    }

    @Override
    public SubscriptionDTO cancel(Long subscriptionId, String reason) throws LibraryException {
        Subscription sub = findOwned(subscriptionId);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.PENDING) {
            throw new SubscriptionException("Subscription is already " + sub.getStatus(), HttpStatus.CONFLICT);
        }
        // Borrow rights stop immediately.
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setCancellationReason(reason == null || reason.isBlank() ? "Cancelled by user" : reason.trim());
        sub.setAutoRenew(false);
        paymentService.cancelPendingFor(PaymentReferenceType.SUBSCRIPTION, sub.getId(), "Subscription cancelled");

        return SubscriptionMapper.toDTO(subscriptionRepository.save(sub));
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO getById(Long subscriptionId) throws LibraryException {
        return SubscriptionMapper.toDTO(findOwned(subscriptionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionDTO> getHistory(Long userId) throws LibraryException {
        User user = resolveTargetUser(userId);
        return SubscriptionMapper.toDTOList(subscriptionRepository.findByUserOrderByCreatedAtDesc(user));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO getActive(Long userId) throws LibraryException {
        User user = resolveTargetUser(userId);
        return findValidSubscription(user)
                .map(SubscriptionMapper::toDTO)
                .orElseThrow(() -> new SubscriptionException(
                        "No active subscription found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidSubscription(Long userId) throws LibraryException {
        return findValidSubscription(resolveTargetUser(userId)).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<SubscriptionDTO> getAllValid(int page, int size) throws LibraryException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new SubscriptionException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<Subscription> result = subscriptionRepository.findAllValid(
                LocalDate.now(), PageRequest.of(page, size, Sort.by("endDate").ascending()));
        return new PagesResponse<>(
                SubscriptionMapper.toDTOList(result.getContent()),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isLast(), result.isFirst(), result.isEmpty());
    }

    @Override
    public int deactivateExpired() {
        return subscriptionRepository.expireEndedSubscriptions(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findValidSubscription(User user) {
        return subscriptionRepository.findValid(user, LocalDate.now()).stream().findFirst();
    }

    // ------------------------------------------------------------------ helpers

    /** Loads a subscription the current user is allowed to touch (their own, or any for admins). */
    private Subscription findOwned(Long subscriptionId) throws LibraryException {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(
                        "Subscription not found with id: " + subscriptionId, HttpStatus.NOT_FOUND));
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !sub.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return sub;
    }

    /** null or own id -> current user; another id -> admins only. */
    private User resolveTargetUser(Long userId) throws LibraryException {
        User current = userService.getCurrentUser();
        if (userId == null || userId.equals(current.getId())) {
            return current;
        }
        if (!current.isAdmin()) {
            throw new AccessDeniedException("Access denied");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
    }

    /** Only one unpaid request at a time: older PENDING ones are cancelled. */
    private void cancelUnpaidSubscriptions(User user) {
        for (Subscription pending : subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.PENDING)) {
            pending.setStatus(SubscriptionStatus.CANCELLED);
            pending.setCancelledAt(LocalDateTime.now());
            pending.setCancellationReason("Replaced by a newer subscription request before payment");
            paymentService.cancelPendingFor(PaymentReferenceType.SUBSCRIPTION, pending.getId(),
                    "Replaced by a newer subscription request");
        }
    }
}
