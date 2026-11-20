package com.library.repository;

import com.library.model.EmailVerificationToken;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from EmailVerificationToken t where t.expiryDate < :now")
    int deleteExpired(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
