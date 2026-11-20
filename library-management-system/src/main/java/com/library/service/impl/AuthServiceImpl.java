package com.library.service.impl;

import com.library.configirations.JwtProvider;
import com.library.domain.AuthProvider;
import com.library.domain.UserRole;
import com.library.exception.UserException;
import com.library.mapper.UserMapper;
import com.library.model.EmailVerificationToken;
import com.library.model.PasswordResetToken;
import com.library.model.User;
import com.library.payload.dto.UserDTO;
import com.library.payload.response.AuthResponse;
import com.library.repository.EmailVerificationTokenRepository;
import com.library.repository.PasswordResetTokenRepository;
import com.library.repository.UserRepository;
import com.library.service.AuthService;
import com.library.service.EmailService;
import com.library.service.GoogleTokenVerifier;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
@Transactional(rollbackFor = UserException.class)
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_HOURS = 24;

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;

    @Value("${app.reset-token.expiration-minutes:15}")
    private long resetTokenExpirationMinutes;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:9090}")
    private String backendUrl;

    /** Development only (see application.properties). */
    @Value("${app.dev.log-reset-links:false}")
    private boolean logLinks;

    // ------------------------------------------------------------------ login / signup

    @Override
    public AuthResponse login(String email, String password) throws UserException {
        User user = userRepository.findByEmail(email);

        // Same message for unknown email, wrong password and Google-only accounts (no password),
        // so nobody can probe which emails exist.
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new UserException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLogin(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser, "Login successful", "Welcome back " + savedUser.getFullName());
    }

    @Override
    public AuthResponse signup(UserDTO req) throws UserException {
        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new UserException("Email is already registered", HttpStatus.CONFLICT);
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
        sendVerificationEmail(savedUser);

        return buildAuthResponse(savedUser, "Account created successfully. Check your email to confirm your address.",
                "Welcome " + savedUser.getFullName());
    }

    /**
     * Google already proved who the user is. Cases:
     *  - an account already linked to this Google id      -> log in
     *  - an account with the same email, not yet linked   -> link it, but ONLY if Google says the email is verified
     *  - no account                                       -> create one (verified, no password)
     */
    @Override
    public AuthResponse loginWithGoogle(String idToken) throws UserException {
        GoogleTokenVerifier.GoogleUser g = googleTokenVerifier.verify(idToken);
        if (!g.emailVerified() || g.email() == null) {
            throw new UserException("Your Google email address is not verified", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByGoogleId(g.googleId());
        String title;
        if (user == null) {
            user = userRepository.findByEmail(g.email());
            if (user != null) {
                user.setGoogleId(g.googleId());
                user.setVerified(true);   // Google confirmed this email
                title = "Your Google account is now linked";
            } else {
                user = User.builder()
                        .email(g.email())
                        .fullName(g.name() != null && !g.name().isBlank() ? g.name() : g.email())
                        .googleId(g.googleId())
                        .authProvider(AuthProvider.GOOGLE)
                        .role(UserRole.ROLE_USER)
                        .verified(true)
                        .build();
                title = "Welcome " + user.getFullName();
            }
        } else {
            title = "Welcome back " + user.getFullName();
        }
        if (user.getProfileImage() == null && g.pictureUrl() != null) {
            user.setProfileImage(g.pictureUrl());
        }
        user.setLastLogin(LocalDateTime.now());
        User saved = userRepository.save(user);
        return buildAuthResponse(saved, "Login with Google successful", title);
    }

    // ------------------------------------------------------------------ email verification

    @Override
    public String verifyEmail(String token) throws UserException {
        EmailVerificationToken t = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException("This verification link is invalid or was already used"));
        if (t.isExpired()) {
            verificationTokenRepository.delete(t);
            throw new UserException("This verification link has expired. Ask for a new one from your account.");
        }
        User user = t.getUser();
        user.setVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(t);
        return user.getEmail();
    }

    @Override
    public void resendVerification() throws UserException {
        User user = userService.getCurrentUser();
        if (Boolean.TRUE.equals(user.getVerified())) {
            throw new UserException("Your email is already verified", HttpStatus.CONFLICT);
        }
        sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) {
        verificationTokenRepository.deleteByUser(user);
        EmailVerificationToken t = verificationTokenRepository.save(EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(VERIFICATION_HOURS))
                .build());
        String link = backendUrl + "/auth/verify-email?token=" + t.getToken();
        emailService.sendEmailVerification(user, link);
        if (logLinks) {
            log.info("[DEV] Email verification link for user {}: {}", user.getId(), link);
        }
    }

    // ------------------------------------------------------------------ password reset

    @Override
    public void createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            // Do not reveal whether the email exists.
            log.info("Password reset requested for an unknown email");
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);
        PasswordResetToken resetToken = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes))
                .build());

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        emailService.sendPasswordReset(user, resetLink);
        if (logLinks) {
            log.info("[DEV] Password reset link for user {}: {}", user.getId(), resetLink);
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) throws UserException {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new UserException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());   // every login token issued before now stops working
        user.setVerified(true);                           // they proved they can read mail at this address
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private AuthResponse buildAuthResponse(User user, String message, String title) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority(user.getRole().name())));

        AuthResponse response = new AuthResponse();
        response.setJwt(jwtProvider.generateToken(auth));
        response.setMessage(message);
        response.setTitle(title);
        response.setUser(UserMapper.toDTO(user));
        return response;
    }
}
