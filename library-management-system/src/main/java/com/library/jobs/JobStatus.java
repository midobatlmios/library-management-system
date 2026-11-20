package com.library.jobs;

import java.time.LocalDateTime;

/** What the admin sees for each background job. */
public record JobStatus(
        String name,
        String description,
        String schedule,
        LocalDateTime nextRun,
        boolean running,
        LocalDateTime lastStartedAt,
        LocalDateTime lastFinishedAt,
        Long lastDurationMs,
        Integer lastResultCount,
        String lastError,
        long totalRuns) {
}
