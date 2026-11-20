package com.library.controller;

import com.library.exception.UserException;
import com.library.payload.dto.UserDTO;
import com.library.payload.request.ForgotPasswordRequest;
import com.library.payload.request.GoogleLoginRequest;
import com.library.payload.request.LoginRequest;
import com.library.payload.request.ResetPasswordRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.AuthResponse;
import com.library.service.AuthService;
import com.library.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttempts;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody UserDTO req) throws UserException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http)
            throws UserException {
        String ip = http.getRemoteAddr();
        loginAttempts.checkLoginAllowed(req.getEmail(), ip);      // 429 after too many failures
        try {
            AuthResponse response = authService.login(req.getEmail(), req.getPassword());
            loginAttempts.loginSucceeded(req.getEmail(), ip);
            return ResponseEntity.ok(response);
        } catch (UserException e) {
            loginAttempts.loginFailed(req.getEmail(), ip);
            throw e;
        }
    }

    /** Body: { "idToken": "<credential from the Google sign-in button>" } */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest req) throws UserException {
        return ResponseEntity.ok(authService.loginWithGoogle(req.getIdToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                                      HttpServletRequest http) throws UserException {
        loginAttempts.checkResetRequest(req.getEmail(), http.getRemoteAddr());
        authService.createPasswordResetToken(req.getEmail());
        return ResponseEntity.ok(new ApiResponse(
                "If this email is registered, a reset link was sent to it.", true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) throws UserException {
        authService.resetPassword(req.getToken(), req.getPassword());
        return ResponseEntity.ok(new ApiResponse("Password reset successful", true));
    }

    /** The link in the verification email. Opened in a browser, so it answers with a small HTML page. */
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam(defaultValue = "") String token) {
        try {
            String email = authService.verifyEmail(token);
            return SimpleHtml.message(HttpStatus.OK, true, "Email confirmed",
                    "Thank you! " + email + " is now verified.", frontendUrl);
        } catch (UserException e) {
            return SimpleHtml.message(HttpStatus.BAD_REQUEST, false, "Link not valid", e.getMessage(), frontendUrl);
        }
    }

    /** Logged-in users only. Limited like forgot-password. */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerification(HttpServletRequest http) throws UserException {
        loginAttempts.checkResetRequest("verify:" + http.getRemoteAddr(), http.getRemoteAddr());
        authService.resendVerification();
        return ResponseEntity.ok(new ApiResponse("A new verification email was sent", true));
    }
}
