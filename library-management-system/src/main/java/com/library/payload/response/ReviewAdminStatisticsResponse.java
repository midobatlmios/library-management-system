package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAdminStatisticsResponse {
    /** Active reviews (deleted ones are not counted). */
    private long totalReviews;
    private long deletedReviews;
    private long verifiedReviews;
    private BigDecimal averageRating;
    private long reviewsLast30Days;
}
