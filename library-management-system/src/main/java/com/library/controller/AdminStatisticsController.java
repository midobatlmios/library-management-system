package com.library.controller;

import com.library.service.ActivityStatisticsService;
import com.library.service.ActivityStatisticsService.DailyActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Data for the admin dashboard's charts. Everything under /api/admin/** is admin-only. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final ActivityStatisticsService activityStatistics;

    /** Example: GET /api/admin/statistics/daily?days=30 (1 to 365 days, today included). */
    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DailyActivityResponse> daily(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(activityStatistics.daily(days));
    }
}
