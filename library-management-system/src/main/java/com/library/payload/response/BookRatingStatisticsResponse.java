package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRatingStatisticsResponse {
    private Long bookId;
    private String bookTitle;
    /** Rounded to 1 decimal, e.g. 4.6. Zero when there are no reviews. */
    private BigDecimal averageRating;
    private long totalReviews;
    /** Always has keys "5" to "1". */
    private Map<String, Long> ratingDistribution;
    private long verifiedReaderReviews;
}
