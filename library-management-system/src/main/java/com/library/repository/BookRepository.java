package com.library.repository;

import com.library.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book,Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);


    @Query("select b from Book b where " +
            "(:searchTerm is null OR " +
            " lower(b.title) like lower(concat('%', :searchTerm, '%')) OR " +
            " lower(b.author) like lower(concat('%', :searchTerm, '%')) OR " +
            " lower(b.isbn) like lower(concat('%', :searchTerm, '%'))) AND " +
            "(:allGenres = true or b.genre.id in :genreIds) AND " +
            "(:availableOnly = false or b.availableCopies > 0) AND " +
            "(:includeInactive = true or b.active = true)")

    /**
     * allGenres = true ignores genreIds. Otherwise only books whose genre is in genreIds
     * (the chosen genre plus its sub-genres). genreIds must never be empty: an empty
     * IN list is invalid SQL, so callers pass a dummy value when allGenres is true.
     */
    Page<Book> saerchBooksWithFilter(
            @Param("searchTerm") String searchTerm,
            @Param("allGenres") boolean allGenres,
            @Param("genreIds") Collection<Long> genreIds,
            @Param("availableOnly") boolean availableOnly,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable
    );

    long countByActiveTrue();

    /** [genreId, number of active books] per genre that has any. */
    @Query("select b.genre.id, count(b) from Book b where b.active = true group by b.genre.id")
    java.util.List<Object[]> countActiveBooksByGenre();

    /** Books (active or not) that use this genre. */
    long countByGenre_Id(Long genreId);

    @Query("select count(b) from Book b where b.availableCopies>0 and b.active=true")
    long countAvailableBooks();

    /**
     * Takes one copy in a single SQL statement, so two people can never both get the last copy.
     * Returns 1 if a copy was taken, 0 if none was available.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Book b set b.availableCopies = b.availableCopies - 1 " +
            "where b.id = :bookId and b.availableCopies > 0 and b.active = true")
    int takeOneCopy(@Param("bookId") Long bookId);

    /** Puts one copy back, never above totalCopies. */
    @Modifying(flushAutomatically = true)
    @Query("update Book b set b.availableCopies = b.availableCopies + 1 " +
            "where b.id = :bookId and b.availableCopies < b.totalCopies")
    int returnOneCopy(@Param("bookId") Long bookId);

}
