package com.library.payload.response;

import com.library.payload.dto.UserDTO;

public class AuthResponse {
    private String jwt;
    private String message;
    private String title;
    private UserDTO user;
}
