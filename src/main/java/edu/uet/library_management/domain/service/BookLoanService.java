package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookLoanCreateRequest;
import edu.uet.library_management.domain.dto.BookLoanDto;

import java.util.List;

public interface BookLoanService {
    BookLoanDto createLoan(BookLoanCreateRequest request, String currentUserEmail);
    BookLoanDto returnBook(Long loanId);
    List<BookLoanDto> getMyLoans(String email);
    List<BookLoanDto> getAllLoans();
    BookLoanDto getLoanById(Long id);
    boolean refreshDatabase();
}
