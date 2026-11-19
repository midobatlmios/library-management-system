package com.library.configirations;

import com.library.domain.AuthProvider;
import com.library.domain.UserRole;
import com.library.model.User;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account at startup if it does not exist yet.
 * Credentials come from app.admin.* in application.properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.full-name:Library Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail) != null) {
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(UserRole.ROLE_ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .verified(true)
                .build();
        userRepository.save(admin);
        log.info("Created default admin account: {}", adminEmail);
    }
}
