package com.library.service;

import com.library.exception.LibraryException;
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

import java.util.List;
import java.util.Optional;

public interface FineService {

    // ---- create ----
    FineDTO createFine(CreateFineRequest request) throws LibraryException;

    /** Called by Book Loan on return: creates an OVERDUE fine if the book came back late. */
    Optional<Fine> createOverdueFineIfLate(BookLoan loan);

    // ---- pay / waive ----
    /**
     * Starts an online payment of the outstanding amount and returns its checkoutUrl.
     * The fine is marked PAID automatically when the payment succeeds.
     */
    PaymentInitiateResponse initiatePayment(Long fineId) throws LibraryException;

    /** Admin records a payment made at the desk. Partial payments allowed. Amount in minor units. */
    FineDTO recordPayment(Long fineId, Long amount, String transactionId) throws LibraryException;

    FineDTO waive(WaiveFineRequest request) throws LibraryException;

    // ---- read ----
    FineDTO getById(Long fineId) throws LibraryException;
    List<FineDTO> getByBookLoan(Long bookLoanId) throws LibraryException;
    PagesResponse<FineDTO> getMyFines(String status, String type, int page, int size) throws LibraryException;
    PagesResponse<FineDTO> getAllFines(Long userId, String status, String type, int page, int size) throws LibraryException;

    // ---- totals (minor units) ----
    TotalFinesResponse getMyTotalUnpaid() throws LibraryException;
    TotalFinesResponse getUserTotalUnpaid(Long userId) throws LibraryException;
    HasUnpaidFinesResponse hasUnpaidFines(Long userId) throws LibraryException;
    TotalFinesResponse getTotalCollected();
    TotalFinesResponse getTotalOutstanding();

    /** For Book Loan: does this user owe money? */
    boolean hasUnpaidFines(User user);
    long getOutstandingAmount(User user);

    void delete(Long fineId) throws LibraryException;
}
