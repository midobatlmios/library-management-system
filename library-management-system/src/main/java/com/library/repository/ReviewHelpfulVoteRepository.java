package com.library.repository;

import com.library.model.BookReview;
import com.library.model.ReviewHelpfulVote;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {
    boolean existsByReviewAndUser(BookReview review, User user);

    void deleteByReview(BookReview review);
}
