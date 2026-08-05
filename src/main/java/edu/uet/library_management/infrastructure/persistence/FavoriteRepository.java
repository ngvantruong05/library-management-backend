package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndBookId(Long userId, Long bookId);
    Optional<Favorite> findByUserEmailAndBookId(String email, Long bookId);
    List<Favorite> findByUserEmail(String email);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserEmailAndBookId(String email, Long bookId);
    void deleteByUserEmailAndBookId(String email, Long bookId);
}
