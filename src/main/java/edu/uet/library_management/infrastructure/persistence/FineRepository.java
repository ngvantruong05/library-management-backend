package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.enums.FineStatus;
import edu.uet.library_management.domain.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    List<Fine> findByBookLoanUserEmail(String email);
    List<Fine> findByBookLoanUserId(Long userId);
    List<Fine> findByStatus(FineStatus status);
}
