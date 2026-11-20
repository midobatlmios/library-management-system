package com.library.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.library.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Checks a "Sign in with Google" ID token: signed by Google (public keys fetched and cached by the
 * library), issued for OUR client id, not expired. Only then do we trust the email inside it.
 */
@Slf4j
@Service
public class GoogleTokenVerifier {

    @Value("${app.google.client-id:}")
    private String clientId;

    private volatile GoogleIdTokenVerifier verifier;

    public record GoogleUser(String googleId, String email, boolean emailVerified, String name, String pictureUrl) {
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public GoogleUser verify(String idToken) throws UserException {
        if (!isConfigured()) {
            throw new UserException("Login with Google is not configured on this server", HttpStatus.SERVICE_UNAVAILABLE);
        }
        GoogleIdToken token;
        try {
            token = verifier().verify(idToken);
        } catch (Exception e) {
            log.warn("Google token check failed: {}", e.getMessage());
            token = null;
        }
        if (token == null) {
            logWhyRejected(idToken);
            throw new UserException("Invalid Google sign-in. Please try again.", HttpStatus.UNAUTHORIZED);
        }
        GoogleIdToken.Payload p = token.getPayload();
        return new GoogleUser(
                p.getSubject(),
                p.getEmail(),
                Boolean.TRUE.equals(p.getEmailVerified()),
                (String) p.get("name"),
                (String) p.get("picture"));
    }

    /**
     * The Google library only says "invalid". Log WHICH check failed (no email, no token, nothing personal):
     * audience (our client id?), issuer, and the token times compared with this server's clock.
     */
    private void logWhyRejected(String idToken) {
        try {
            GoogleIdToken parsed = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idToken);
            GoogleIdToken.Payload p = parsed.getPayload();
            long now = System.currentTimeMillis() / 1000;
            Object aud = p.getAudience();
            boolean audOk = aud != null && aud.toString().contains(clientId);
            boolean issOk = "accounts.google.com".equals(p.getIssuer()) || "https://accounts.google.com".equals(p.getIssuer());
            Long iat = p.getIssuedAtTimeSeconds();
            Long exp = p.getExpirationTimeSeconds();
            log.warn("Google token rejected. audience matches our client id: {} | issuer ok: {} ({}) | "
                            + "server clock minus token issue time: {} s | seconds until expiry: {} | "
                            + "if all are fine, the signature check failed (or Google's keys could not be downloaded)",
                    audOk, issOk, p.getIssuer(), iat == null ? "?" : now - iat, exp == null ? "?" : exp - now);
        } catch (Exception e) {
            log.warn("Google token rejected and could not be read: {}", e.getMessage());
        }
    }

    private GoogleIdTokenVerifier verifier() {
        if (verifier == null) {
            synchronized (this) {
                if (verifier == null) {
                    verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                            .setAudience(List.of(clientId))
                            .build();
                }
            }
        }
        return verifier;
    }
}
