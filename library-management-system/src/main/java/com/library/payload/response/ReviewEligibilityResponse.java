package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEligibilityResponse {
    private boolean canReview;
    /** Why not, when canReview is false. */
    private String reason;
}
