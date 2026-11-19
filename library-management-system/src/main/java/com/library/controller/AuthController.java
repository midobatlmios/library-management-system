package com.library.controller;

import com.library.exception.UserException;
import com.library.payload.dto.UserDTO;
import com.library.payload.request.ForgotPasswordRequest;
import com.library.payload.request.LoginRequest;
import com.library.payload.request.ResetPasswordRequest;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.AuthResponse;
import com.library.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody UserDTO req) throws UserException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) throws UserException {
        return ResponseEntity.ok(authService.login(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.createPasswordResetToken(req.getEmail());
        return ResponseEntity.ok(new ApiResponse(
                "If this email is registered, a reset link was sent to it.", true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) throws UserException {
        authService.resetPassword(req.getToken(), req.getPassword());
        return ResponseEntity.ok(new ApiResponse("Password reset successful", true));
    }
}
