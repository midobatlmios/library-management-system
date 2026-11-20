package com.library.service;

import com.library.exception.UserException;
import com.library.payload.dto.UserDTO;
import com.library.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse login(String email, String password) throws UserException;

    /** Creates the account and emails a verification link. */
    AuthResponse signup(UserDTO req) throws UserException;

    /** Verifies a Google ID token and logs in (creating or linking the account if needed). */
    AuthResponse loginWithGoogle(String idToken) throws UserException;

    void createPasswordResetToken(String email);
    void resetPassword(String token, String newPassword) throws UserException;

    /** Marks the account's email as verified. Returns the email address. */
    String verifyEmail(String token) throws UserException;

    /** Sends a new verification email to the logged-in user. */
    void resendVerification() throws UserException;
}
