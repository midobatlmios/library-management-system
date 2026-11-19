package com.library.service.impl;

import com.library.configirations.JwtProvider;
import com.library.domain.AuthProvider;
import com.library.domain.UserRole;
import com.library.exception.UserException;
import com.library.mapper.UserMapper;
import com.library.model.PasswordResetToken;
import com.library.model.User;
import com.library.payload.dto.UserDTO;
import com.library.payload.response.AuthResponse;
import com.library.repository.PasswordResetTokenRepository;
import com.library.repository.UserRepository;
import com.library.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reset-token.expiration-minutes:15}")
    private long resetTokenExpirationMinutes;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public AuthResponse login(String email, String password) throws UserException {
        User user = userRepository.findByEmail(email);

        // Same message for unknown email and wrong password, so nobody can probe which emails exist.
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new UserException("Invalid credentials");
        }

        user.setLastLogin(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser, "Login successful", "Welcome back " + savedUser.getFullName());
    }

    @Override
    public AuthResponse signup(UserDTO req) throws UserException {
        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new UserException("Email is already registered");
        }

        User createdUser = new User();
        createdUser.setEmail(req.getEmail());
        createdUser.setPassword(passwordEncoder.encode(req.getPassword()));
        createdUser.setPhone(req.getPhone());
        createdUser.setFullName(req.getFullName());
        createdUser.setLastLogin(LocalDateTime.now());
        createdUser.setRole(UserRole.ROLE_USER); // never trust a role sent by the client
        createdUser.setAuthProvider(AuthProvider.LOCAL);
        createdUser.setVerified(false);

        User savedUser = userRepository.save(createdUser);

        return buildAuthResponse(savedUser, "Account created successfully", "Welcome " + savedUser.getFullName());
    }

    @Override
    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            // Do not reveal whether the email exists.
            log.info("Password reset requested for an unknown email");
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // TODO (step 12): send this link by email instead of logging it.
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        log.info("Password reset link for {}: {}", user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) throws UserException {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new UserException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private AuthResponse buildAuthResponse(User user, String message, String title) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority(user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwtProvider.generateToken(auth));
        response.setMessage(message);
        response.setTitle(title);
        response.setUser(UserMapper.toDTO(user));
        return response;
    }
}
