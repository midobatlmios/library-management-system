package com.library.repository;

import com.library.model.Book;
import com.library.model.User;
import com.library.model.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserAndBook(User user, Book book);

    boolean existsByUserAndBook(User user, Book book);

    Page<Wishlist> findByUser(User user, Pageable pageable);

    long countByUser(User user);

    long countByBook(Book book);
}
