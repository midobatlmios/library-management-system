package com.library.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** The "credential" string the Google sign-in button gives the frontend. */
@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
