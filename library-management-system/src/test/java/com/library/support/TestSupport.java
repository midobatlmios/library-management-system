package com.library.support;

import com.jayway.jsonpath.JsonPath;
import com.library.repository.UserRepository;
import com.library.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base class for API tests: real HTTP requests through security, controllers, services and a real
 * (separate) MariaDB database. Every test creates its own users/books with unique names, so tests
 * don't depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TestSupport {

    protected static final String TEST_CARD_OK = "4242424242424242";
    protected static final String TEST_CARD_DECLINED = "4000000000000002";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UserRepository userRepository;

    /** Tests never send real emails; tests can also check which emails would have been sent. */
    @MockitoBean
    protected EmailService emailService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${app.admin.password}")
    private String adminPassword;

    @BeforeEach
    void neverTouchTheRealDatabase() {
        assertThat(datasourceUrl)
                .as("Tests must run on the test database, never on library_sys")
                .contains("library_sys_test");
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected MvcResult call(MockHttpServletRequestBuilder request, String token, String jsonBody) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (jsonBody != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mvc.perform(request).andReturn();
    }

    protected int status(MockHttpServletRequestBuilder request, String token, String jsonBody) throws Exception {
        return call(request, token, jsonBody).getResponse().getStatus();
    }

    protected static String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    protected static <T> T json(MvcResult r, String path) throws Exception {
        return JsonPath.read(body(r), path);
    }

    protected static Long id(MvcResult r, String path) throws Exception {
        Object v = JsonPath.read(body(r), path);
        return v == null ? null : ((Number) v).longValue();
    }

    protected static String unique() {
        return UUID.randomUUID().toString().substring(0, 8) + COUNTER.incrementAndGet();
    }

    // ------------------------------------------------------------------ users

    protected String adminToken() throws Exception {
        return login("admin@library.local", adminPassword);
    }

    protected String login(String email, String password) throws Exception {
        MvcResult r = call(post("/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
        assertThat(r.getResponse().getStatus()).as("login " + email).isEqualTo(200);
        return json(r, "$.jwt");
    }

    /** Signs up a user, marks the email verified, and returns { email, token }. */
    protected TestUser newUser() throws Exception {
        String email = "user" + unique() + "@example.com";
        MvcResult r = call(post("/auth/signup"), null,
                "{\"fullName\":\"Test User\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(email));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        jdbc.update("update users set verified = 1 where email = ?", email);
        Long id = id(r, "$.user.id");
        return new TestUser(id, email, json(r, "$.jwt"));
    }

    public record TestUser(Long id, String email, String token) {
    }

    // ------------------------------------------------------------------ catalog / plans / subscriptions

    protected Long newGenre() throws Exception {
        // Genre codes allow only A-Z and _, so digits become letters (0 -> A ... 9 -> J).
        StringBuilder code = new StringBuilder("G_");
        for (char c : unique().toUpperCase().toCharArray()) {
            code.append(Character.isDigit(c) ? (char) ('A' + (c - '0')) : c);
        }
        MvcResult r = call(post("/api/genres"), adminToken(),
                "{\"code\":\"%s\",\"name\":\"Genre %s\"}".formatted(code, code));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    protected Long newBook(int copies) throws Exception {
        String isbn = "978" + String.format("%010d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 10_000_000_000L));
        MvcResult r = call(post("/api/books"), adminToken(),
                "{\"isbn\":\"%s\",\"title\":\"Book %s\",\"author\":\"Author\",\"genreId\":%d,\"totalCopies\":%d}"
                        .formatted(isbn, unique(), newGenre(), copies));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    protected Long newPlan(int maxBooks, int maxBorrowDays, String price) throws Exception {
        MvcResult r = call(post("/api/subscription-plans"), adminToken(),
                "{\"title\":\"Plan %s\",\"price\":%s,\"durationInDays\":30,\"maxBooksAllowed\":%d,\"maxBorrowDays\":%d}"
                        .formatted(unique(), price, maxBooks, maxBorrowDays));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    /** Subscribes and pays with the success test card. Returns the subscription id. */
    protected Long subscribeAndPay(TestUser user, Long planId) throws Exception {
        MvcResult r = call(post("/api/subscriptions/subscribe"), user.token(), "{\"planId\":%d}".formatted(planId));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        String paymentId = json(r, "$.paymentId");
        assertThat(payWithCard(paymentId, TEST_CARD_OK)).isEqualTo(200);
        return id(r, "$.referenceId");
    }

    /** A subscribed user (10 books, 14 days). */
    protected TestUser subscribedUser() throws Exception {
        TestUser u = newUser();
        subscribeAndPay(u, newPlan(10, 14, "100"));
        return u;
    }

    /** Submits the simulated checkout form. Returns the HTTP status of the result page. */
    protected int payWithCard(String paymentId, String card) throws Exception {
        return mvc.perform(post("/pay/" + paymentId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("cardNumber", card).param("expiry", "12/35").param("cvc", "123"))
                .andReturn().getResponse().getStatus();
    }

    protected Long checkout(TestUser user, Long bookId) throws Exception {
        MvcResult r = call(post("/api/book-loans/checkout"), user.token(), "{\"bookId\":%d}".formatted(bookId));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    protected int availableCopies(Long bookId) {
        return jdbc.queryForObject("select available_copies from book where id = ?", Integer.class, bookId);
    }

    /** Pretends a loan started `checkedOutDaysAgo` days ago and was due `dueDaysAgo` days ago. */
    protected void makeLate(Long loanId, int checkedOutDaysAgo, int dueDaysAgo) {
        jdbc.update("update book_loans set checkout_date = date_sub(curdate(), interval ? day), "
                + "due_date = date_sub(curdate(), interval ? day) where id = ?", checkedOutDaysAgo, dueDaysAgo, loanId);
    }
}
