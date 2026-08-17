package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.DashboardStatsDto;
import edu.uet.library_management.domain.service.BookLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final BookLoanService bookLoanService;

    @GetMapping
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(bookLoanService.getDashboardStats());
    }
}
