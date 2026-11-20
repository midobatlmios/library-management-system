package com.library.controller;

import com.library.service.GoogleTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * DEVELOPMENT ONLY: http://localhost:9090/dev/google-login shows a real "Sign in with Google" button
 * that calls POST /auth/google, so Google login can be tested before the React app exists.
 * Turned off with app.dev.google-test-page=false (it then answers 404).
 */
@RestController
@RequiredArgsConstructor
public class DevGoogleLoginPageController {

    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.dev.google-test-page:false}")
    private boolean enabled;

    @GetMapping(value = "/dev/google-login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page() {
        if (!enabled) {
            return SimpleHtml.message(HttpStatus.NOT_FOUND, false, "Not found", "This page is disabled.", null);
        }
        if (!googleTokenVerifier.isConfigured()) {
            return SimpleHtml.message(HttpStatus.SERVICE_UNAVAILABLE, false, "Google login not configured",
                    "Set app.google.client-id in secrets.properties and restart.", null);
        }

        byte[] n = new byte[16];
        new SecureRandom().nextBytes(n);
        String nonce = Base64.getEncoder().encodeToString(n);

        String body = """
                <h1>Login with Google (test page)</h1>
                <p class="muted">Development only. Click the button, choose your Google account, and see what
                the backend answers.</p>
                <div id="g_id_onload" data-client_id="%s" data-callback="onGoogleSignIn"
                     data-auto_prompt="false"></div>
                <div class="g_id_signin" data-type="standard" data-size="large" data-text="signin_with"></div>
                <h2 style="font-size:1rem;margin-top:24px">Backend answer</h2>
                <pre id="out">(not signed in yet)</pre>
                <script src="https://accounts.google.com/gsi/client" async nonce="%s"></script>
                <script nonce="%s">
                  function onGoogleSignIn(response) {
                    var out = document.getElementById('out');
                    out.textContent = 'Sending the Google token to POST /auth/google ...';
                    fetch('/auth/google', {
                      method: 'POST',
                      headers: {'Content-Type': 'application/json'},
                      body: JSON.stringify({idToken: response.credential})
                    }).then(function (r) { return r.json().then(function (j) { return [r.status, j]; }); })
                      .then(function (res) {
                        var j = res[1];
                        if (j.jwt) { j.jwt = j.jwt.substring(0, 20) + '... (hidden)'; }
                        out.textContent = 'HTTP ' + res[0] + '\\n' + JSON.stringify(j, null, 2);
                      })
                      .catch(function (e) { out.textContent = 'Error: ' + e; });
                  }
                </script>
                """.formatted(SimpleHtml.esc(googleTokenVerifier.getClientId()), nonce, nonce);

        String csp = "default-src 'none'; "
                + "script-src 'nonce-" + nonce + "' https://accounts.google.com/gsi/client; "
                + "frame-src https://accounts.google.com/gsi/; "
                + "connect-src 'self' https://accounts.google.com/gsi/; "
                + "style-src 'unsafe-inline' https://accounts.google.com/gsi/style; "
                + "img-src https: data:";
        return SimpleHtml.page(HttpStatus.OK, "Login with Google - test", body, csp);
    }
}
