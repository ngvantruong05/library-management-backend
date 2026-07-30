package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.BookCopyDto;
import edu.uet.library_management.domain.service.BookCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book-copies")
@RequiredArgsConstructor
public class BookCopyController {

    private final BookCopyService bookCopyService;

    @GetMapping("/book/{bookId}")
    public ResponseEntity<BookCopyDto> getCopyByBookId(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookCopyService.getCopyByBookId(bookId));
    }

    @PutMapping("/book/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookCopyDto> updateCopy(@PathVariable Long bookId, @RequestParam(name = "totalCopies") int totalCopies) {
        return ResponseEntity.ok(bookCopyService.updateCopy(bookId, totalCopies));
    }
}
