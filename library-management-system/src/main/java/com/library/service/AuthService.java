package com.library.service;

import com.library.exception.UserException;
import com.library.payload.dto.UserDTO;
import com.library.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse login(String email, String password) throws UserException;
    AuthResponse signup(UserDTO req) throws UserException;

    void createPasswordResetToken(String email);
    void resetPassword(String token, String newPassword) throws UserException;
}
