package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatisticsResponse {
    private long totalLoans;
    /** Not returned yet (CHECKED_OUT + OVERDUE). */
    private long activeLoans;
    private long checkedOutLoans;
    private long overdueLoans;
    private long returnedLoans;
    /** Returned after the due date. */
    private long returnedLate;
    private long checkoutsLast30Days;
    /** Not-returned loans whose due date has passed but the overdue job has not marked yet. */
    private long lateButNotMarked;
}
