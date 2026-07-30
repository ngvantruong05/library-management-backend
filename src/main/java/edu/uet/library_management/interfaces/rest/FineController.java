package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.FineDto;
import edu.uet.library_management.domain.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/my-fines")
    public ResponseEntity<List<FineDto>> getMyFines(Authentication authentication) {
        return ResponseEntity.ok(fineService.getMyFines(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FineDto>> getAllFines() {
        return ResponseEntity.ok(fineService.getAllFines());
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FineDto> payFine(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.payFine(id));
    }
}
