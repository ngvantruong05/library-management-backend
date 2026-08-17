package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.model.BookLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {
    List<BookLoan> findByUserEmail(String email);
    List<BookLoan> findByUserId(Long userId);
    List<BookLoan> findByBookId(Long bookId);
    List<BookLoan> findByValid(boolean valid);
    List<BookLoan> findByStatus(LoanStatus status);
    List<BookLoan> findByStatusAndDueDateBefore(LoanStatus status, LocalDateTime dateTime);

    @Query("SELECT b.id AS bookId, b.title AS title, b.thumbnail AS thumbnail, COUNT(bl) AS loanCount " +
           "FROM BookLoan bl JOIN bl.book b " +
           "GROUP BY b.id, b.title, b.thumbnail " +
           "ORDER BY loanCount DESC")
    List<Object[]> findTopLentBooks(Pageable pageable);

    List<BookLoan> findByValidTrueOrderByLastUpdatedDesc(Pageable pageable);
}
