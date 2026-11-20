package com.library.jobs;

import com.library.exception.LibraryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Runs background jobs and remembers how each run went.
 *  - The same job never runs twice at the same time (a manual run while the scheduled one is busy is refused).
 *  - A failing job is logged and recorded; it never stops the other jobs.
 */
@Slf4j
@Component
public class JobRunner {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    private static final class Job {
        final String name;
        final String description;
        final String cron;
        final IntSupplier work;
        final AtomicBoolean running = new AtomicBoolean(false);
        volatile LocalDateTime lastStartedAt;
        volatile LocalDateTime lastFinishedAt;
        volatile Long lastDurationMs;
        volatile Integer lastResultCount;
        volatile String lastError;
        volatile long totalRuns;

        Job(String name, String description, String cron, IntSupplier work) {
            this.name = name;
            this.description = description;
            this.cron = cron;
            this.work = work;
        }
    }

    public void register(String name, String description, String cron, IntSupplier work) {
        jobs.put(name, new Job(name, description, cron, work));
    }

    /** Runs a job now. Returns false if it was already running. */
    public boolean run(String name, String trigger) {
        Job job = jobs.get(name);
        if (job == null) {
            log.error("Unknown job {}", name);
            return false;
        }
        if (!job.running.compareAndSet(false, true)) {
            log.warn("Job {} is already running; {} run skipped", name, trigger);
            return false;
        }
        LocalDateTime start = LocalDateTime.now();
        job.lastStartedAt = start;
        try {
            int count = job.work.getAsInt();
            job.lastResultCount = count;
            job.lastError = null;
            if (count > 0) {
                log.info("Job {} ({}) done: {} item(s)", name, trigger, count);
            } else {
                log.debug("Job {} ({}) done: nothing to do", name, trigger);
            }
        } catch (Exception e) {
            job.lastResultCount = null;
            job.lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("Job {} ({}) failed", name, trigger, e);
        } finally {
            job.lastFinishedAt = LocalDateTime.now();
            job.lastDurationMs = Duration.between(start, job.lastFinishedAt).toMillis();
            job.totalRuns++;
            job.running.set(false);
        }
        return true;
    }

    /** Manual run from the admin API. */
    public JobStatus runManually(String name) throws LibraryException {
        if (!jobs.containsKey(name)) {
            throw new JobException("Unknown job '" + name + "'. Jobs: " + jobs.keySet(), HttpStatus.NOT_FOUND);
        }
        if (!run(name, "manual")) {
            throw new JobException("Job '" + name + "' is already running", HttpStatus.CONFLICT);
        }
        return status(jobs.get(name));
    }

    public List<JobStatus> statuses() {
        List<JobStatus> list = new ArrayList<>();
        jobs.values().stream().sorted((a, b) -> a.name.compareTo(b.name)).forEach(j -> list.add(status(j)));
        return list;
    }

    private static JobStatus status(Job j) {
        LocalDateTime next = null;
        try {
            next = CronExpression.parse(j.cron).next(LocalDateTime.now());
        } catch (Exception ignored) {
            // not a cron schedule
        }
        return new JobStatus(j.name, j.description, j.cron, next, j.running.get(), j.lastStartedAt,
                j.lastFinishedAt, j.lastDurationMs, j.lastResultCount, j.lastError, j.totalRuns);
    }

    /** Small checked exception so the global handler returns the right status. */
    public static class JobException extends LibraryException {
        public JobException(String message, HttpStatus status) {
            super(message, status);
        }
    }
}
