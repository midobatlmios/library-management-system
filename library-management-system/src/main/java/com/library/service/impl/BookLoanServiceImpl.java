package com.library.service.impl;

import com.library.domain.BookLoanStatus;
import com.library.domain.BookLoanType;
import com.library.exception.BookException;
import com.library.exception.BookLoanException;
import com.library.exception.LibraryException;
import com.library.exception.UserException;
import com.library.mapper.BookLoanMapper;
import com.library.model.Book;
import com.library.model.BookLoan;
import com.library.model.Subscription;
import com.library.model.User;
import com.library.payload.dto.BookLoanDTO;
import com.library.payload.request.CheckinRequest;
import com.library.payload.request.CheckoutRequest;
import com.library.payload.request.LoanSearchRequest;
import com.library.payload.request.RenewLoanRequest;
import com.library.payload.request.UpdateLoanRequest;
import com.library.payload.response.LoanStatisticsResponse;
import com.library.payload.response.PagesResponse;
import com.library.repository.BookLoanRepository;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import com.library.model.Fine;
import com.library.service.BookLoanService;
import com.library.service.FineService;
import com.library.service.ReservationQueueService;
import com.library.model.Reservation;
import java.util.Optional;
import com.library.service.SubscriptionService;
import com.library.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.library.repository.BookLoanSpecifications.*;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = LibraryException.class)
public class BookLoanServiceImpl implements BookLoanService {

    private static final Set<BookLoanStatus> NOT_RETURNED = EnumSet.of(BookLoanStatus.CHECKED_OUT, BookLoanStatus.OVERDUE);
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "checkoutDate", "dueDate", "returnDate", "status", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;
    private static final int SHORT_TERM_MAX_DAYS = 7;

    private final BookLoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final FineService fineService;
    private final ReservationQueueService reservationQueue;
    private final com.library.service.EmailService emailService;

    // ------------------------------------------------------------------ checkout

    @Override
    public BookLoanDTO checkout(CheckoutRequest request, Long userId) throws LibraryException {
        User borrower = userId == null ? userService.getCurrentUser() : findUser(userId);

        // 1. A valid subscription gives the borrow limit and the loan length.
        Subscription subscription = subscriptionService.findValidSubscription(borrower)
                .orElseThrow(() -> new BookLoanException(
                        "An active subscription is required to borrow books"));

        // 2. No new books while holding late ones, or while owing fines.
        LocalDate today = LocalDate.now();
        if (loanRepository.hasLateLoans(borrower, today)) {
            throw new BookLoanException("Please return your overdue books before borrowing new ones");
        }
        if (fineService.hasUnpaidFines(borrower)) {
            throw new BookLoanException("Please pay your outstanding fines ("
                    + Fine.toMajor(fineService.getOutstandingAmount(borrower)) + " " + subscription.getCurrency()
                    + ") before borrowing new books");
        }

        // 3. Borrow limit from the plan.
        long borrowedNow = loanRepository.countByUserAndStatusIn(borrower, NOT_RETURNED);
        if (!subscription.canBorrowBooks((int) borrowedNow)) {
            throw new BookLoanException("Borrow limit reached: " + borrowedNow + " of "
                    + subscription.getMaxBooksAllowed() + " books allowed by plan '" + subscription.getPlanName() + "'");
        }

        // 4. The book.
        Book book = bookRepository.findById(request.getBookId())
                .filter(Book::getActive)
                .orElseThrow(() -> new BookException(
                        "Book not found with id: " + request.getBookId(), HttpStatus.NOT_FOUND));
        if (loanRepository.existsByUserAndBookAndStatusIn(borrower, book, NOT_RETURNED)) {
            throw new BookLoanException("This user already has a copy of '" + book.getTitle() + "'",
                    HttpStatus.CONFLICT);
        }

        // 5. Loan length.
        int maxDays = subscription.getMaxBorrowDays();
        int dueDays = request.getDueDays() != null ? request.getDueDays() : maxDays;
        if (dueDays > maxDays) {
            throw new BookLoanException("Due days cannot exceed " + maxDays + " for plan '"
                    + subscription.getPlanName() + "'");
        }

        // 6. Get a copy: one held for this user by a reservation, or one from the shelf.
        //    The shelf copy is taken in one SQL statement, so two users can't get the last copy.
        Optional<Reservation> held = reservationQueue.findHeldFor(borrower, book);
        if (held.isEmpty() && bookRepository.takeOneCopy(book.getId()) == 0) {
            throw new BookLoanException("No copies of '" + book.getTitle()
                    + "' are available right now. You can reserve it.", HttpStatus.CONFLICT);
        }

        BookLoan loan = new BookLoan();
        loan.setUser(borrower);
        loan.setBook(book);
        loan.setType(request.getType() != null ? request.getType()
                : (dueDays <= SHORT_TERM_MAX_DAYS ? BookLoanType.SHORT_TERM : BookLoanType.NORMAL));
        loan.setStatus(BookLoanStatus.CHECKED_OUT);
        loan.setCheckoutDate(today);
        loan.setDueDate(today.plusDays(dueDays));
        loan.setNotes(request.getNotes());

        BookLoan saved = loanRepository.save(loan);
        held.ifPresent(r -> reservationQueue.markFulfilled(r, saved.getId()));
        return BookLoanMapper.toDTO(saved);
    }

    // ------------------------------------------------------------------ return

    @Override
    public BookLoanDTO checkin(CheckinRequest request) throws LibraryException {
        BookLoan loan = findOwned(request.getBookLoanId());
        if (!loan.isActive()) {
            throw new BookLoanException("This book was already returned on " + loan.getReturnDate(),
                    HttpStatus.CONFLICT);
        }

        loan.setReturnDate(LocalDate.now());
        loan.refreshOverdue();               // stores the final number of late days
        loan.setStatus(BookLoanStatus.RETURNED);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            loan.setNotes(request.getNotes().trim());
        }
        // The copy goes to the next person waiting for this book, or back on the shelf.
        reservationQueue.onCopyFreed(loan.getBook());

        BookLoan saved = loanRepository.save(loan);
        // Late return -> OVERDUE fine (rate and cap in application.properties: app.fine.*).
        fineService.createOverdueFineIfLate(saved);
        return BookLoanMapper.toDTO(saved);
    }

    // ------------------------------------------------------------------ renew

    @Override
    public BookLoanDTO renew(RenewLoanRequest request) throws LibraryException {
        BookLoan loan = findOwned(request.getBookLoanId());

        if (!loan.isActive()) {
            throw new BookLoanException("Only books that are still checked out can be renewed",
                    HttpStatus.CONFLICT);
        }
        if (loan.isLate()) {
            throw new BookLoanException("Cannot renew overdue loan", HttpStatus.CONFLICT);
        }
        if (loan.getRenewalCount() >= loan.getMaxRenewals()) {
            throw new BookLoanException("Renewal limit reached (" + loan.getMaxRenewals() + " renewals)",
                    HttpStatus.CONFLICT);
        }

        Subscription subscription = subscriptionService.findValidSubscription(loan.getUser())
                .orElseThrow(() -> new BookLoanException("An active subscription is required to renew books"));
        int maxDays = subscription.getMaxBorrowDays();
        int extendDays = request.getExtendDays() != null ? request.getExtendDays() : maxDays;
        if (extendDays > maxDays) {
            throw new BookLoanException("Extend days cannot exceed " + maxDays + " for plan '"
                    + subscription.getPlanName() + "'");
        }
        if (reservationQueue.hasWaiting(loan.getBook())) {
            throw new BookLoanException("Cannot renew: other users are waiting for this book",
                    HttpStatus.CONFLICT);
        }

        loan.setDueDate(loan.getDueDate().plusDays(extendDays));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        return BookLoanMapper.toDTO(loanRepository.save(loan));
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public BookLoanDTO getById(Long loanId) throws LibraryException {
        return BookLoanMapper.toDTO(findOwned(loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookLoanDTO> getMyLoans(String status, int page, int size) throws LibraryException {
        return loansOf(userService.getCurrentUser(), status, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookLoanDTO> getUserLoans(Long userId, String status, int page, int size)
            throws LibraryException {
        return loansOf(findUser(userId), status, page, size);
    }

    private PagesResponse<BookLoanDTO> loansOf(User user, String status, int page, int size) throws LibraryException {
        Pageable pageable = pageable(page, size, "checkoutDate", "DESC");
        Set<BookLoanStatus> statuses = parseStatus(status);
        Page<BookLoan> result = statuses == null
                ? loanRepository.findByUser(user, pageable)
                : loanRepository.findByUserAndStatusIn(user, statuses, pageable);
        return toPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<BookLoanDTO> search(LoanSearchRequest r) throws LibraryException {
        Specification<BookLoan> spec = allOf(
                userId(r.getUserId()),
                bookId(r.getBookId()),
                statusIn(parseStatus(r.getStatus())),
                type(r.getType()),
                lateNow(r.getOverdueOnly()),
                between("checkoutDate", r.getCheckoutFrom(), r.getCheckoutTo()),
                between("dueDate", r.getDueFrom(), r.getDueTo()));
        return toPage(loanRepository.findAll(spec,
                pageable(r.getPage(), r.getSize(), r.getSortBy(), r.getSortDirection())));
    }

    // ------------------------------------------------------------------ admin

    @Override
    public BookLoanDTO update(Long loanId, UpdateLoanRequest request) throws LibraryException {
        BookLoan loan = findLoan(loanId);

        if (request.getDueDate() != null) {
            if (request.getDueDate().isBefore(loan.getCheckoutDate())) {
                throw new BookLoanException("Due date cannot be before the checkout date (" + loan.getCheckoutDate() + ")");
            }
            loan.setDueDate(request.getDueDate());
        }
        if (request.getMaxRenewals() != null) {
            if (request.getMaxRenewals() < loan.getRenewalCount()) {
                throw new BookLoanException("Max renewals cannot be lower than renewals already used ("
                        + loan.getRenewalCount() + ")");
            }
            loan.setMaxRenewals(request.getMaxRenewals());
        }
        if (request.getNotes() != null) {
            loan.setNotes(request.getNotes());
        }

        // A new due date may make the loan late, or no longer late.
        if (loan.isActive()) {
            loan.setStatus(BookLoanStatus.CHECKED_OUT);
        }
        loan.refreshOverdue();
        return BookLoanMapper.toDTO(loanRepository.save(loan));
    }

    @Override
    public int sendDueReminders(int daysBefore) {
        List<BookLoan> loans = loanRepository.findNeedingDueReminder(LocalDate.now().plusDays(daysBefore));
        for (BookLoan loan : loans) {
            emailService.sendDueSoonReminder(loan);          // sent after this transaction commits
            loan.setDueReminderSentFor(loan.getDueDate());   // never twice for the same due date
        }
        loanRepository.saveAll(loans);
        return loans.size();
    }

    @Override
    public int updateOverdueLoans() {
        List<BookLoan> late = loanRepository.findLateUnreturned(LocalDate.now());
        late.forEach(BookLoan::refreshOverdue);
        loanRepository.saveAll(late);
        return late.size();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanStatisticsResponse getStatistics() {
        long checkedOut = loanRepository.countByStatus(BookLoanStatus.CHECKED_OUT);
        long overdue = loanRepository.countByStatus(BookLoanStatus.OVERDUE);
        LocalDate today = LocalDate.now();
        return new LoanStatisticsResponse(
                loanRepository.count(),
                checkedOut + overdue,
                checkedOut,
                overdue,
                loanRepository.countByStatus(BookLoanStatus.RETURNED),
                loanRepository.countReturnedLate(),
                loanRepository.countByCheckoutDateGreaterThanEqual(today.minusDays(30)),
                loanRepository.countLateButNotMarked(today));
    }

    // ------------------------------------------------------------------ helpers

    private BookLoan findLoan(Long loanId) throws BookLoanException {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new BookLoanException("BookLoan not found: " + loanId, HttpStatus.NOT_FOUND));
    }

    /** The borrower or an admin. */
    private BookLoan findOwned(Long loanId) throws LibraryException {
        BookLoan loan = findLoan(loanId);
        User current = userService.getCurrentUser();
        if (!current.isAdmin() && !loan.getUser().getId().equals(current.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return loan;
    }

    private User findUser(Long userId) throws UserException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with id: " + userId, HttpStatus.NOT_FOUND));
    }

    private static Set<BookLoanStatus> parseStatus(String status) throws BookLoanException {
        if (status == null || status.isBlank()) {
            return null;
        }
        String s = status.trim().toUpperCase();
        if (s.equals("ACTIVE")) {
            return NOT_RETURNED;
        }
        try {
            return EnumSet.of(BookLoanStatus.valueOf(s));
        } catch (IllegalArgumentException e) {
            throw new BookLoanException("Unknown status '" + status
                    + "'. Use ACTIVE, CHECKED_OUT, OVERDUE or RETURNED");
        }
    }

    private static Pageable pageable(int page, int size, String sortBy, String direction) throws BookLoanException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BookLoanException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BookLoanException("Cannot sort by '" + sortBy + "'. Allowed: " + SORTABLE_FIELDS);
        }
        Sort sort = "ASC".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort.and(Sort.by("id").descending()));
    }

    private static PagesResponse<BookLoanDTO> toPage(Page<BookLoan> page) {
        return new PagesResponse<>(
                BookLoanMapper.toDTOList(page.getContent()),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isLast(), page.isFirst(), page.isEmpty());
    }

    /** AND of all filters that are set (null filters are skipped). */
    @SafeVarargs
    private static Specification<BookLoan> allOf(Specification<BookLoan>... specs) {
        List<Specification<BookLoan>> active = new ArrayList<>();
        for (Specification<BookLoan> s : specs) {
            if (s != null) active.add(s);
        }
        return (root, query, cb) -> cb.and(active.stream()
                .map(s -> s.toPredicate(root, query, cb))
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new));
    }
}
