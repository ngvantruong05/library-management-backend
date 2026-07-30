package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    Optional<BookCopy> findByBookId(Long bookId);
    boolean existsByBookId(Long bookId);
}
