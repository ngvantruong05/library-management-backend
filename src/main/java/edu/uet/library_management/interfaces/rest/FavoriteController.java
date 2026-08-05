package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.BookDto;
import edu.uet.library_management.domain.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{bookId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long bookId, Authentication authentication) {
        favoriteService.addFavorite(bookId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long bookId, Authentication authentication) {
        favoriteService.removeFavorite(bookId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<BookDto>> getFavorites(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.getFavorites(authentication.getName()));
    }

    @GetMapping("/check/{bookId}")
    public ResponseEntity<Map<String, Boolean>> isFavorite(@PathVariable Long bookId, Authentication authentication) {
        boolean isFavorite = favoriteService.isFavorite(bookId, authentication.getName());
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }
}
