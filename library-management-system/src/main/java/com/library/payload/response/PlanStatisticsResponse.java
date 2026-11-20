package com.library.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStatisticsResponse {
    private long totalPlans;
    private long activePlans;
    private long inactivePlans;
    /** Subscriptions valid today, across all plans. */
    private long activeSubscriptions;
    /** Plan popularity: planCode -> subscriptions valid today. */
    private Map<String, Long> activeSubscriptionsByPlan;
}
