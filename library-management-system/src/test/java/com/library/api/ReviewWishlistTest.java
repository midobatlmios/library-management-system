package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Reviews (only real readers) and wishlists (private, no duplicates). */
class ReviewWishlistTest extends TestSupport {

    private TestUser readerOf(Long book) throws Exception {
        TestUser u = subscribedUser();
        Long loan = checkout(u, book);
        call(post("/api/book-loans/check-in"), u.token(), "{\"bookLoanId\":%d}".formatted(loan));
        return u;
    }

    private Long review(TestUser u, Long book, int rating) throws Exception {
        MvcResult r = call(post("/api/reviews"), u.token(),
                "{\"bookId\":%d,\"rating\":%d,\"reviewText\":\"A genuinely useful read.\"}".formatted(book, rating));
        assertThat(r.getResponse().getStatus()).as(body(r)).isEqualTo(201);
        return id(r, "$.id");
    }

    @Test
    void onlyReadersWhoReturnedTheBookCanReview() throws Exception {
        Long book = newBook(1);
        TestUser stranger = newUser();
        assertThat((Boolean) json(call(get("/api/reviews/can-review/" + book), stranger.token(), null), "$.canReview"))
                .isFalse();
        assertThat(status(post("/api/reviews"), stranger.token(),
                "{\"bookId\":%d,\"rating\":5,\"reviewText\":\"Never read it, still great\"}".formatted(book)))
                .isEqualTo(400);

        TestUser reader = readerOf(book);
        review(reader, book, 5);
        assertThat(status(post("/api/reviews"), reader.token(),
                "{\"bookId\":%d,\"rating\":4,\"reviewText\":\"Second opinion here\"}".formatted(book)))
                .as("one review per book").isEqualTo(409);
    }

    @Test
    void helpfulVotesFollowTheRules() throws Exception {
        Long book = newBook(1);
        TestUser author = readerOf(book);
        Long reviewId = review(author, book, 4);
        TestUser voter = newUser();

        assertThat(status(post("/api/reviews/" + reviewId + "/helpful"), author.token(), null))
                .as("own review").isEqualTo(400);
        assertThat(status(post("/api/reviews/" + reviewId + "/helpful"), voter.token(), null)).isEqualTo(200);
        assertThat(status(post("/api/reviews/" + reviewId + "/helpful"), voter.token(), null))
                .as("vote twice").isEqualTo(409);
        assertThat((Integer) json(call(get("/api/reviews/" + reviewId), null, null), "$.helpfulCount")).isEqualTo(1);
    }

    @Test
    void reviewsArePublicButNeverShowEmails() throws Exception {
        Long book = newBook(1);
        TestUser reader = readerOf(book);
        review(reader, book, 5);
        MvcResult r = call(get("/api/reviews/book/" + book), null, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(r)).doesNotContain(reader.email());
    }

    @Test
    void wishlistHasNoDuplicatesAndIsPrivate() throws Exception {
        TestUser owner = newUser();
        Long book = newBook(1);
        assertThat(status(post("/api/wishlist/add/" + book), owner.token(), null)).isEqualTo(201);
        assertThat(status(post("/api/wishlist/add/" + book), owner.token(), null)).isEqualTo(409);
        assertThat((Boolean) json(call(get("/api/wishlist/check/" + book), owner.token(), null), "$.isInWishlist"))
                .isTrue();

        assertThat(status(get("/api/wishlist/user/" + owner.id()), newUser().token(), null)).isEqualTo(403);
        assertThat(status(get("/api/wishlist/user/" + owner.id()), adminToken(), null)).isEqualTo(200);
    }
}
