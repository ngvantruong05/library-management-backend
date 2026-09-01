package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    List<Book> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a LEFT JOIN b.categories c " +
           "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Book> searchBooks(@Param("query") String query);

    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a LEFT JOIN b.categories c " +
                   "WHERE (:query IS NULL OR :query = '' OR " +
                   "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "AND (:categoryId IS NULL OR c.id = :categoryId)",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b LEFT JOIN b.authors a LEFT JOIN b.categories c " +
                        "WHERE (:query IS NULL OR :query = '' OR " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND (:categoryId IS NULL OR c.id = :categoryId)")
    Page<Book> searchBooksPaginated(@Param("query") String query, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT b FROM Book b LEFT JOIN Rating r ON r.book = b " +
                   "WHERE (:query IS NULL OR :query = '' OR " +
                   "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM b.categories c)) " +
                   "GROUP BY b " +
                   "ORDER BY COALESCE(AVG(r.score), 0.0) DESC, COUNT(r) DESC",
           countQuery = "SELECT COUNT(b) FROM Book b " +
                        "WHERE (:query IS NULL OR :query = '' OR " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM b.categories c))")
    Page<Book> searchBooksOrderByRatingDesc(@Param("query") String query, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT b FROM Book b LEFT JOIN Rating r ON r.book = b " +
                   "WHERE (:query IS NULL OR :query = '' OR " +
                   "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM b.categories c)) " +
                   "GROUP BY b " +
                   "ORDER BY COALESCE(AVG(r.score), 0.0) ASC, COUNT(r) ASC",
           countQuery = "SELECT COUNT(b) FROM Book b " +
                        "WHERE (:query IS NULL OR :query = '' OR " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND (:categoryId IS NULL OR :categoryId IN (SELECT c.id FROM b.categories c))")
    Page<Book> searchBooksOrderByRatingAsc(@Param("query") String query, @Param("categoryId") Long categoryId, Pageable pageable);

    long countByActivated(boolean activated);
}

