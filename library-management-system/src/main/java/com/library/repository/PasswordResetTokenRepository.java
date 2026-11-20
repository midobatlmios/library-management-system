package com.library.repository;

import com.library.model.PasswordResetToken;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from PasswordResetToken t where t.expiryDate < :now")
    int deleteExpired(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
