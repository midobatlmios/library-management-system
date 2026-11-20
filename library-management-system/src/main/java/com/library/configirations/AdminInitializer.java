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

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Makes sure the admin account exists, with the password from secrets.properties / ADMIN_PASSWORD.
 *  - Password configured: the admin is created, or its password is updated to match (the configured
 *    value is the source of truth, so changing it in secrets.properties changes the admin password).
 *  - Nothing configured and no admin yet: a random password is generated and printed once in the log.
 *  - Nothing configured and the admin exists: nothing changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.full-name:Library Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        boolean configured = adminPassword != null && !adminPassword.isBlank();
        User admin = userRepository.findByEmail(adminEmail);

        if (admin != null) {
            if (configured && !passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPasswordChangedAt(LocalDateTime.now());   // old admin tokens stop working
                userRepository.save(admin);
                log.info("Admin password for {} updated from configuration", adminEmail);
            }
            return;
        }

        String password = configured ? adminPassword : randomPassword();
        userRepository.save(User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(password))
                .fullName(adminFullName)
                .role(UserRole.ROLE_ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .verified(true)
                .build());
        if (configured) {
            log.info("Created admin account {}", adminEmail);
        } else {
            log.warn("Created admin account {} with a GENERATED password: {}  "
                    + "(shown once; set app.admin.password in secrets.properties to choose your own)",
                    adminEmail, password);
        }
    }

    private static String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("A1");
        for (int i = 0; i < 18; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
