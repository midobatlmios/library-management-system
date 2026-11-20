package com.library.configirations;

import com.library.model.User;
import com.library.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Reads "Authorization: Bearer <token>" on every request.
 * Besides checking the signature and expiry, it loads the user from the database, so:
 *  - a deleted user's token stops working,
 *  - tokens issued before a password change are rejected,
 *  - the role always comes from the database (a role change applies immediately).
 */
public class JwtValidator extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtValidator(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(JwtConstant.JWT_HEADER);

        if (header != null && header.startsWith("Bearer ")) {
            User user;
            try {
                Claims claims = jwtProvider.parse(header);
                user = userRepository.findByEmail(String.valueOf(claims.get("email")));
                if (user == null || issuedBeforePasswordChange(claims.getIssuedAt(), user)) {
                    throw new IllegalStateException("token no longer valid");
                }
            } catch (Exception e) {
                // Expired, tampered, revoked or unknown user: answer 401 instead of crashing with 500.
                SecurityContextHolder.clearContext();
                SecurityConfig.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    /** JWT times are whole seconds, so compare at second precision. */
    private static boolean issuedBeforePasswordChange(Date issuedAt, User user) {
        if (user.getPasswordChangedAt() == null || issuedAt == null) {
            return false;
        }
        long changed = user.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return issuedAt.getTime() < changed;
    }
}
