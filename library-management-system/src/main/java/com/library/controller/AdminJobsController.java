package com.library.controller;

import com.library.exception.LibraryException;
import com.library.jobs.JobRunner;
import com.library.jobs.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Background jobs: see when each ran and what it did, or run one now. Admin only. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/jobs")
public class AdminJobsController {

    private final JobRunner jobRunner;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<JobStatus>> list() {
        return ResponseEntity.ok(jobRunner.statuses());
    }

    /** Example: POST /api/admin/jobs/overdue-loans/run */
    @PostMapping("/{name}/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobStatus> run(@PathVariable String name) throws LibraryException {
        return ResponseEntity.ok(jobRunner.runManually(name));
    }
}
