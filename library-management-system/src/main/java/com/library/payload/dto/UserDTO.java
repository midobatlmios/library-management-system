package com.library.payload.dto;

import com.library.domain.AuthProvider;
import com.library.domain.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String phone;
    @NotBlank(message = "Full name is required")
    private String fullName;
    // Read-only fields: the server sets these, anything the client sends is ignored.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UserRole role;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AuthProvider authProvider;
    private String profileImage;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean verified;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastLogin;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}