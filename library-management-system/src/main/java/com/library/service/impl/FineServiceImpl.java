package com.library.service.impl;

import com.library.domain.FineStatus;
import com.library.domain.FineType;
import com.library.exception.BookLoanException;
import com.library.exception.FineException;
import com.library.exception.LibraryException;
import com.library.exception.UserException;
import com.library.mapper.FineMapper;
import com.library.model.BookLoan;
import com.library.model.Fine;
import com.library.model.User;
import com.library.payload.dto.FineDTO;
import com.library.payload.request.CreateFineRequest;
import com.library.payload.request.WaiveFineRequest;
import com.library.payload.response.HasUnpaidFinesResponse;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.PaymentInitiateResponse;
import com.library.payload.response.TotalFinesResponse;
import com.library.repository.BookLoanRepository;
import com.library.repository.FineRepository;
import com.library.repository.UserRepository;
import com.library.service.FineService;
import com.library.service.UserService;
import com.library.service.PaymentService;
import com.library.domain.PaymentReferenceType;
import com.library.model.Payment;
import com.library.payment.PaymentTargetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class FineServiceImpl implements FineService, PaymentTargetHandler {

    private static final int MAX_PAGE_SIZE = 100;

    private final FineRepository fineRepository;
    private final BookLoanRepository loanRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PaymentService paymentService;

    /** Overdue fine per late day, minor units (500 = 5.00 MAD). */
    @Value("${app.fine.overdue-per-day:500}")
    private long overduePerDay;

    /** Maximum overdue fine for one loan, minor units (10000 = 100.00 MAD). */
    @Value("${app.fine.overdue-max:10000}")
    private long overdueMax;

    @Value("${app.default-currency:MAD}")
    private String currency;

    // ------------------------------------------------------------------ create

    @Override
    public FineDTO createFine(CreateFineRequest request) throws LibraryException {
        BookLoan loan = loanRepository.findById(request.getBookLoanId())
                .orElseThrow(() -> new BookLoanException(
                        "BookLoan not found: " + request.getBookLoanId(), HttpStatus.NOT_FOUND));

        User borrower = loan.getUser();
        if (request.getUserId() != null && !request.getUserId().equals(borrower.getId())) {
            throw new FineException("User " + request.getUserId() + " is not the borrower of loan " + loan.getId());
        }
        if (request.getType() == FineType.OVERDUE && fineRepository.existsByBookLoanAndType(loan, FineType.OVERDUE)) {
            throw new FineException("This loan already has an overdue fine", HttpStatus.CONFLICT);
        }

        Fine fine = Fine.builder()
                .bookLoan(loan)
                .user(borrower)
                .type(request.getType())
                .amount(request.getAmount())
                .currency(currency)
                .reason(request.getReason())
                .notes(request.getNotes())
                .build();
        return FineMapper.toDTO(fineRepository.save(fine));
    }

    @Override
    public Optional<Fine> createOverdueFineIfLate(BookLoan loan) {
        int lateDays = loan.getCurrentOverdueDays();
        if (lateDays <= 0 || fineRepository.existsByBookLoanAndType(loan, FineType.OVERDUE)) {
            return Optional.empty();
        }
        long amount = Math.min(lateDays * overduePerDay, overdueMax);
        Fine fine = Fine.builder()
                .bookLoan(loan)
                .user(loan.getUser())
                .type(FineType.OVERDUE)
                .amount(amount)
                .currency(currency)
                .reason("Returned " + lateDays + " day(s) late")
                .build();
        return Optional.of(fineRepository.save(fine));
    }

    // ------------------------------------------------------------------ pay / waive

    /** Starts an online payment for the outstanding amount; returns the checkout link. */
    @Override
    public PaymentInitiateResponse initiatePayment(Long fineId) throws LibraryException {
        Fine fine = findOwned(fineId);
        requireUnpaid(fine);

        PaymentInitiateResponse payment = paymentService.create(fine.getUser(), PaymentReferenceType.FINE,
                fine.getId(), fine.getAmountOutstanding(), fine.getCurrency(),
                "Fine (" + fine.getType() + "): " + fine.getBookLoan().getBook().getTitle());
        fine.setPendingPaymentId(payment.getPaymentId());
        fineRepository.save(fine);
        return payment;
    }

    // ------------------------------------------------------------------ payment hooks (PaymentTargetHandler)

    @Override
    public PaymentReferenceType type() {
        return PaymentReferenceType.FINE;
    }

    @Override
    public void checkStillPayable(Payment payment) throws LibraryException {
        Fine fine = findFine(payment.getReferenceId());
        if (!fine.isPending()) {
            throw new FineException("fine is already " + fine.getStatus(), HttpStatus.CONFLICT);
        }
        if (!payment.getPaymentId().equals(fine.getPendingPaymentId())) {
            throw new FineException("a newer payment was started for this fine", HttpStatus.CONFLICT);
        }
        if (fine.getAmountOutstanding() != payment.getAmount()) {
            throw new FineException("the amount owed has changed, please start a new payment", HttpStatus.CONFLICT);
        }
    }

    /** Only the payment system calls this, after a successful charge. */
    @Override
    public void onPaymentSucceeded(Payment payment) throws LibraryException {
        checkStillPayable(payment);
        Fine fine = findFine(payment.getReferenceId());
        fine.applyPayment(payment.getAmount(), payment.getPaymentId(), null);
        fineRepository.save(fine);
    }

    @Override
    public FineDTO recordPayment(Long fineId, Long amount, String transactionId) throws LibraryException {
        Fine fine = findFine(fineId);
        requireUnpaid(fine);
        if (amount == null || amount < 1) {
            throw new FineException("Amount must be at least 1");
        }
        if (amount > fine.getAmountOutstanding()) {
            throw new FineException("Amount " + amount + " is more than the outstanding " + fine.getAmountOutstanding());
        }
        String reference = (transactionId == null || transactionId.isBlank()) ? "DESK_" + newPaymentId() : transactionId.trim();
        fine.applyPayment(amount, reference, userService.getCurrentUser());
        // The amount owed changed, so an online payment link for the old amount must not work any more.
        paymentService.cancelPendingFor(PaymentReferenceType.FINE, fine.getId(), "Fine partly paid at the desk");
        return FineMapper.toDTO(fineRepository.save(fine));
    }

    @Override
    public FineDTO waive(WaiveFineRequest request) throws LibraryException {
        Fine fine = findFine(request.getFineId());
        requireUnpaid(fine);
        fine.waive(userService.getCurrentUser(), request.getWaiverReason().trim());
        paymentService.cancelPendingFor(PaymentReferenceType.FINE, fine.getId(), "Fine waived");
        return FineMapper.toDTO(fineRepository.save(fine));
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public FineDTO getById(Long fineId) throws LibraryException {
        return FineMapper.toDTO(findOwned(fineId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FineDTO> getByBookLoan(Long bookLoanId) throws LibraryException {
        BookLoan loan = loanRepository.findById(bookLoanId)
                .orElseThrow(() -> new BookLoanException("BookLoan not found: " + bookLoanId, HttpStatus.NOT_FOUND));
        requireSelfOrAdmin(loan.getUser());
        return FineMapper.toDTOList(fineRepository.findByBookLoanOrderByCreatedAtDesc(loan));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<FineDTO> getMyFines(String status, String type, int page, int size) throws LibraryException {
        return search(userService.getCurrentUser(), status, type, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<FineDTO> getAllFines(Long userId, String status, String type, int page, int size)
            throws LibraryException {
        return search(userId == null ? null : findUser(userId), status, type, page, size);
    }

    private PagesResponse<FineDTO> search(User user, String status, String type, int page, int size)
            throws FineException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new FineException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        Page<Fine> result = fineRepository.search(user == null ? null : user.getId(), parse(FineStatus.class, status, "status"),
                parse(FineType.class, type, "type"),
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
        return new PagesResponse<>(FineMapper.toDTOList(result.getContent()),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isLast(), result.isFirst(), result.isEmpty());
    }

    // ------------------------------------------------------------------ totals

    @Override
    @Transactional(readOnly = true)
    public TotalFinesResponse getMyTotalUnpaid() throws LibraryException {
        return total(fineRepository.sumOutstandingForUser(userService.getCurrentUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public TotalFinesResponse getUserTotalUnpaid(Long userId) throws LibraryException {
        User user = findUser(userId);
        requireSelfOrAdmin(user);
        return total(fineRepository.sumOutstandingForUser(user));
    }

    @Override
    @Transactional(readOnly = true)
    public HasUnpaidFinesResponse hasUnpaidFines(Long userId) throws LibraryException {
        User user = findUser(userId);
        requireSelfOrAdmin(user);
        return new HasUnpaidFinesResponse(fineRepository.hasUnpaid(user));
    }

    @Override
    @Transactional(readOnly = true)
    public TotalFinesResponse getTotalCollected() {
        return total(fineRepository.sumCollected());
    }

    @Override
    @Transactional(readOnly = true)
    public TotalFinesResponse getTotalOutstanding() {
        return total(fineRepository.sumOutstanding());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnpaidFines(User user) {
        return fineRepository.hasUnpaid(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long getOutstandingAmount(User user) {
        return fineRepository.sumOutstandingForUser(user);
    }

    // ------------------------------------------------------------------ delete

    @Override
    public void delete(Long fineId) throws LibraryException {
        Fine fine = findFine(fineId);
        paymentService.cancelPendingFor(PaymentReferenceType.FINE, fine.getId(), "Fine deleted");
        fineRepository.delete(fine);
    }

    // ------------------------------------------------------------------ helpers

    private Fine findFine(Long fineId) throws FineException {
        return fineRepository.findById(fineId)
                .orElseThrow(() -> new FineException("Fine not found with id: " + fineId, HttpStatus.NOT_FOUND));
    }

    /** The fined user or an admin. */
    private Fine findOwned(Long fineId) throws LibraryException {
        Fine fine = findFine(fineId);
        requireSelfOrAdmin(fine.getUser());
        return fine;
    }

    private void requireSelfOrAdmin(User owner) throws LibraryException {
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !owner.getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private static void requireUnpaid(Fine fine) throws FineException {
        if (!fine.isPending()) {
            throw new FineException("Fine is already " + fine.getStatus(), HttpStatus.CONFLICT);
        }
    }

    private User findUser(Long userId) throws UserException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
    }

    private TotalFinesResponse total(long minor) {
        return new TotalFinesResponse(minor, Fine.toMajor(minor), currency);
    }

    /** Case-insensitive enum parsing: "pending" and "PENDING" both work. */
    private static <E extends Enum<E>> E parse(Class<E> type, String value, String name) throws FineException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FineException("Unknown " + name + " '" + value + "'. Allowed: "
                    + java.util.Arrays.toString(type.getEnumConstants()));
        }
    }

    private static String newPaymentId() {
        return "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
    }
}
