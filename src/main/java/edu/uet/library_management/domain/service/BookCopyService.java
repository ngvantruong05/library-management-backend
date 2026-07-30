package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookCopyDto;

public interface BookCopyService {
    BookCopyDto getCopyByBookId(Long bookId);
    BookCopyDto updateCopy(Long bookId, int totalCopies);
    void increaseCopy(Long bookId, int count);
}
