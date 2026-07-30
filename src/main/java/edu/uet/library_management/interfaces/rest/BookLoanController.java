package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.BookLoanCreateRequest;
import edu.uet.library_management.domain.dto.BookLoanDto;
import edu.uet.library_management.domain.service.BookLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/book-loans")
@RequiredArgsConstructor
public class BookLoanController {

    private final BookLoanService bookLoanService;

    @PostMapping
    public ResponseEntity<BookLoanDto> createLoan(@Valid @RequestBody BookLoanCreateRequest request, Authentication authentication) {
        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        // If not admin, the user cannot borrow on behalf of another user ID
        if (!isAdmin && request.getUserId() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(bookLoanService.createLoan(request, email));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<BookLoanDto>> getMyLoans(Authentication authentication) {
        return ResponseEntity.ok(bookLoanService.getMyLoans(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookLoanDto>> getAllLoans() {
        return ResponseEntity.ok(bookLoanService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookLoanDto> getLoanById(@PathVariable Long id, Authentication authentication) {
        BookLoanDto dto = bookLoanService.getLoanById(id);
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !dto.getUserEmail().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookLoanDto> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookLoanService.returnBook(id));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refreshDatabase() {
        boolean success = bookLoanService.refreshDatabase();
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Database loan status refreshed successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to refresh loan status"));
        }
    }
}
