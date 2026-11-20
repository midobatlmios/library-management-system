package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Login, signup, tokens and who may call what. */
class AuthSecurityTest extends TestSupport {

    @Test
    void signupIgnoresRoleSentByClientAndNeverReturnsPassword() throws Exception {
        String email = "role" + unique() + "@example.com";
        MvcResult r = call(post("/auth/signup"), null,
                "{\"fullName\":\"Sneaky\",\"email\":\"%s\",\"password\":\"secret123\",\"role\":\"ROLE_ADMIN\",\"verified\":true}"
                        .formatted(email));

        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) json(r, "$.user.role")).isEqualTo("ROLE_USER");
        assertThat((Boolean) json(r, "$.user.verified")).isFalse();
        assertThat(body(r)).doesNotContain("secret123").doesNotContain("\"password\"");
    }

    @Test
    void signupSendsVerificationEmail() throws Exception {
        TestUser u = newUser();
        verify(emailService).sendEmailVerification(argThat(user -> user.getEmail().equals(u.email())),
                contains("/auth/verify-email?token="));
    }

    @Test
    void duplicateEmailIsRefused() throws Exception {
        TestUser u = newUser();
        assertThat(status(post("/auth/signup"), null,
                "{\"fullName\":\"Again\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(u.email())))
                .isEqualTo(409);
    }

    @Test
    void weakPasswordsAreRefused() throws Exception {
        String body = "{\"fullName\":\"Weak\",\"email\":\"weak%s@example.com\",\"password\":\"%s\"}";
        assertThat(status(post("/auth/signup"), null, body.formatted(unique(), "abcdefgh"))).isEqualTo(400); // no digit
        assertThat(status(post("/auth/signup"), null, body.formatted(unique(), "abc1"))).isEqualTo(400);     // too short
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        TestUser u = newUser();
        assertThat(status(post("/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"wrong999\"}".formatted(u.email()))).isEqualTo(401);
    }

    @Test
    void tooManyFailedLoginsAreBlockedEvenWithTheRightPassword() throws Exception {
        TestUser u = newUser();
        String wrong = "{\"email\":\"%s\",\"password\":\"wrong999\"}".formatted(u.email());
        for (int i = 0; i < 5; i++) {
            assertThat(status(post("/auth/login"), null, wrong)).isEqualTo(401);
        }
        assertThat(status(post("/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"secret123\"}".formatted(u.email()))).isEqualTo(429);
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        TestUser u = newUser();
        assertThat(status(get("/api/users/profile"), null, null)).isEqualTo(401);
        String tampered = u.token().substring(0, u.token().length() - 2) + "xx";
        assertThat(status(get("/api/users/profile"), tampered, null)).isEqualTo(401);
        assertThat(status(get("/api/users/profile"), u.token(), null)).isEqualTo(200);
    }

    @Test
    void normalUsersGet403OnAdminEndpointsEvenWithAnInvalidBody() throws Exception {
        TestUser u = newUser();
        assertThat(status(get("/api/users/list"), u.token(), null)).isEqualTo(403);
        assertThat(status(post("/api/books"), u.token(), "{}")).isEqualTo(403);          // not 400
        assertThat(status(post("/api/genres"), u.token(), "{}")).isEqualTo(403);
        assertThat(status(get("/api/admin/jobs"), u.token(), null)).isEqualTo(403);
        assertThat(status(get("/api/users/list"), adminToken(), null)).isEqualTo(200);
    }

    @Test
    void roleIsReadFromTheDatabaseOnEveryRequest() throws Exception {
        TestUser u = newUser();
        assertThat(status(get("/api/users/list"), u.token(), null)).isEqualTo(403);

        jdbc.update("update users set role = 'ROLE_ADMIN' where id = ?", u.id());
        assertThat(status(get("/api/users/list"), u.token(), null)).isEqualTo(200);   // same token

        jdbc.update("update users set role = 'ROLE_USER' where id = ?", u.id());
        assertThat(status(get("/api/users/list"), u.token(), null)).isEqualTo(403);
    }

    @Test
    void passwordResetMakesOldTokensUseless() throws Exception {
        TestUser u = newUser();
        assertThat(status(post("/auth/forgot-password"), null, "{\"email\":\"%s\"}".formatted(u.email()))).isEqualTo(200);
        String resetToken = jdbc.queryForObject(
                "select t.token from password_reset_tokens t join users x on x.id = t.user_id where x.email = ?",
                String.class, u.email());

        Thread.sleep(1100);   // login tokens have whole-second timestamps
        assertThat(status(post("/auth/reset-password"), null,
                "{\"token\":\"%s\",\"password\":\"newpass42\"}".formatted(resetToken))).isEqualTo(200);

        assertThat(status(get("/api/users/profile"), u.token(), null)).isEqualTo(401);           // old token
        assertThat(status(get("/api/users/profile"), login(u.email(), "newpass42"), null)).isEqualTo(200);
        assertThat(status(post("/auth/reset-password"), null,                                     // link works once
                "{\"token\":\"%s\",\"password\":\"another42\"}".formatted(resetToken))).isEqualTo(400);
    }

    @Test
    void emailVerificationLinkWorksOnce() throws Exception {
        String email = "verify" + unique() + "@example.com";
        call(post("/auth/signup"), null,
                "{\"fullName\":\"V\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(email));
        String token = jdbc.queryForObject(
                "select t.token from email_verification_tokens t join users x on x.id = t.user_id where x.email = ?",
                String.class, email);

        assertThat(mvc.perform(get("/auth/verify-email").param("token", token)).andReturn().getResponse().getStatus())
                .isEqualTo(200);
        assertThat(jdbc.queryForObject("select verified from users where email = ?", Boolean.class, email)).isTrue();
        assertThat(mvc.perform(get("/auth/verify-email").param("token", token)).andReturn().getResponse().getStatus())
                .isEqualTo(400);
    }

    @Test
    void forgedGoogleTokenIsRejected() throws Exception {
        assertThat(status(post("/auth/google"), null, "{\"idToken\":\"abc.def.ghi\"}")).isIn(401, 503);
        assertThat(status(post("/auth/google"), null, "{}")).isEqualTo(400);
    }

    @Test
    void everyErrorHasTheStandardShape() throws Exception {
        MvcResult r = call(get("/api/does-not-exist"), adminToken(), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat((Boolean) json(r, "$.success")).isFalse();
        assertThat((String) json(r, "$.message")).isNotBlank();
    }
}
