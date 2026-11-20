package com.library.controller;

import com.library.payload.response.ApiResponse;
import com.library.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Small admin utilities. Everything under /api/admin/** is admin-only. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminToolsController {

    private final EmailService emailService;

    /** Sends a test email right away and reports whether the mail server accepted it. */
    @PostMapping("/email/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> testEmail(@RequestParam String to) {
        if (!to.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(new ApiResponse("'to' must be an email address", false));
        }
        String error = emailService.sendTestNow(to);
        return error == null
                ? ResponseEntity.ok(new ApiResponse("Test email sent", true))
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiResponse("Sending failed: " + error, false));
    }
}
