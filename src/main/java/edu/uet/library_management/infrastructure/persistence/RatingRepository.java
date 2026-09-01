package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserEmailAndBookId(String userEmail, Long bookId);

    Optional<Rating> findByUserIdAndBookId(Long userId, Long bookId);

    Page<Rating> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    List<Rating> findByBookIdOrderByCreatedAtDesc(Long bookId);

    boolean existsByUserEmailAndBookId(String userEmail, Long bookId);

    void deleteByUserEmailAndBookId(String userEmail, Long bookId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.book.id = :bookId")
    Long countByBookId(@Param("bookId") Long bookId);

    @Query("SELECT r.score, COUNT(r) FROM Rating r WHERE r.book.id = :bookId GROUP BY r.score")
    List<Object[]> getRatingDistributionByBookId(@Param("bookId") Long bookId);

    @Query("SELECT r.book.id, AVG(r.score), COUNT(r) FROM Rating r WHERE r.book.id IN :bookIds GROUP BY r.book.id")
    List<Object[]> getRatingStatsForBookIds(@Param("bookIds") Collection<Long> bookIds);

    @Query("SELECT r.book.id, AVG(r.score), COUNT(r) FROM Rating r GROUP BY r.book.id")
    List<Object[]> getAllRatingStats();
}
