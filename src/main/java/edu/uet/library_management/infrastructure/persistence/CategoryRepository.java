package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);

    @Query(value = "SELECT c.id, COUNT(bc.book_id) FROM categories c LEFT JOIN book_categories bc ON c.id = bc.category_id GROUP BY c.id", nativeQuery = true)
    List<Object[]> countBooksPerCategory();

    @Query(value = "SELECT COUNT(bc.book_id) FROM book_categories bc WHERE bc.category_id = :categoryId", nativeQuery = true)
    Long countBooksByCategoryId(@Param("categoryId") Long categoryId);
}
