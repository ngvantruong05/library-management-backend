package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBookIdOrderByCreatedAtAsc(Long bookId);
    List<Comment> findByUserEmail(String email);
}
