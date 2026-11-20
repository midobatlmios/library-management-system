package com.library.service;

import com.library.exception.LibraryException;
import com.library.payload.dto.BookLoanDTO;
import com.library.payload.request.CheckinRequest;
import com.library.payload.request.CheckoutRequest;
import com.library.payload.request.LoanSearchRequest;
import com.library.payload.request.RenewLoanRequest;
import com.library.payload.request.UpdateLoanRequest;
import com.library.payload.response.LoanStatisticsResponse;
import com.library.payload.response.PagesResponse;

public interface BookLoanService {

    /** userId null = the logged-in user borrows; otherwise an admin issues the book to that user. */
    BookLoanDTO checkout(CheckoutRequest request, Long userId) throws LibraryException;

    BookLoanDTO checkin(CheckinRequest request) throws LibraryException;

    BookLoanDTO renew(RenewLoanRequest request) throws LibraryException;

    BookLoanDTO getById(Long loanId) throws LibraryException;

    /** status: CHECKED_OUT, OVERDUE, RETURNED, ACTIVE (= not returned) or null for all. */
    PagesResponse<BookLoanDTO> getMyLoans(String status, int page, int size) throws LibraryException;

    PagesResponse<BookLoanDTO> getUserLoans(Long userId, String status, int page, int size) throws LibraryException;

    PagesResponse<BookLoanDTO> search(LoanSearchRequest request) throws LibraryException;

    BookLoanDTO update(Long loanId, UpdateLoanRequest request) throws LibraryException;

    /** Marks late loans as OVERDUE and refreshes overdue days. Returns how many loans are late. */
    int updateOverdueLoans();

    /** Emails "due soon" reminders for loans due in exactly daysBefore days (once per due date). Returns how many. */
    int sendDueReminders(int daysBefore);

    LoanStatisticsResponse getStatistics();
}
