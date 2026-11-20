package com.library.repository;

import com.library.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book,Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);


    @Query("select b from Book b where " +
            "(:searchTerm is null OR " +
            " lower(b.title) like lower(concat('%', :searchTerm, '%')) OR " +
            " lower(b.author) like lower(concat('%', :searchTerm, '%')) OR " +
            " lower(b.isbn) like lower(concat('%', :searchTerm, '%'))) AND " +
            "(:genreId is null or b.genre.id = :genreId) AND " +
            "(:availableOnly = false or b.availableCopies > 0) AND " +
            "b.active = true")

    Page<Book> saerchBooksWithFilter(
            @Param("searchTerm") String searchTerm,
            @Param("genreId") Long genreId,
            @Param("availableOnly") boolean availableOnly,
            Pageable pageable
    );

    long countByActiveTrue();

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
