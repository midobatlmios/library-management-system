package com.library.api;

import com.library.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Books and genres: public reading, admin-only changes, data rules. */
class CatalogTest extends TestSupport {

    @Test
    void anyoneCanBrowseTheCatalog() throws Exception {
        Long book = newBook(2);
        assertThat(status(get("/api/books"), null, null)).isEqualTo(200);
        MvcResult r = call(get("/api/books/" + book), null, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Integer) json(r, "$.availableCopies")).isEqualTo(2);
        assertThat(status(get("/api/genres/active/hierarchy"), null, null)).isEqualTo(200);
    }

    @Test
    void invalidBooksAreRefused() throws Exception {
        Long genre = newGenre();
        String admin = adminToken();
        assertThat(status(post("/api/books"), admin,
                "{\"isbn\":\"12-34\",\"title\":\"x\",\"author\":\"y\",\"genreId\":%d,\"totalCopies\":1}".formatted(genre)))
                .isEqualTo(400);
        assertThat(status(post("/api/books"), admin,
                "{\"isbn\":\"9780134685991\",\"title\":\"x\",\"author\":\"y\",\"genreId\":%d,\"totalCopies\":1,\"availableCopies\":5}"
                        .formatted(genre)))
                .isEqualTo(400);
    }

    @Test
    void cannotRemoveCopiesThatAreOnLoan() throws Exception {
        Long book = newBook(1);
        checkout(subscribedUser(), book);
        String isbn = jdbc.queryForObject("select isbn from book where id = ?", String.class, book);
        Long genre = jdbc.queryForObject("select genre_id from book where id = ?", Long.class, book);

        assertThat(status(put("/api/books/" + book), adminToken(),
                "{\"isbn\":\"%s\",\"title\":\"t\",\"author\":\"a\",\"genreId\":%d,\"totalCopies\":0}".formatted(isbn, genre)))
                .isEqualTo(409);
    }

    @Test
    void softDeletedBooksAreHiddenFromThePublic() throws Exception {
        Long book = newBook(1);
        assertThat(status(delete("/api/books/" + book), adminToken(), null)).isEqualTo(200);
        assertThat(status(get("/api/books/" + book), null, null)).isEqualTo(404);
        assertThat(status(get("/api/books/" + book), adminToken(), null)).isEqualTo(200);
    }

    @Test
    void genreCannotBePlacedUnderItsOwnChild() throws Exception {
        String admin = adminToken();
        Long parent = newGenre();
        String parentCode = jdbc.queryForObject("select code from genre where id = ?", String.class, parent);
        MvcResult child = call(post("/api/genres"), admin,
                "{\"code\":\"%s_CHILD\",\"name\":\"Child\",\"parentGenreId\":%d}".formatted(parentCode, parent));
        Long childId = id(child, "$.id");

        assertThat(status(put("/api/genres/" + parent), admin,
                "{\"code\":\"%s\",\"name\":\"Parent\",\"parentGenreId\":%d}".formatted(parentCode, childId)))
                .isEqualTo(400);
    }

    @Test
    void genreUsedByBooksCannotBeHardDeleted() throws Exception {
        Long book = newBook(1);
        Long genre = jdbc.queryForObject("select genre_id from book where id = ?", Long.class, book);
        assertThat(status(delete("/api/genres/" + genre + "/hard"), adminToken(), null)).isEqualTo(409);
    }
}
