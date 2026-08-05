package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookDto;
import java.util.List;

public interface FavoriteService {
    void addFavorite(Long bookId, String email);
    void removeFavorite(Long bookId, String email);
    List<BookDto> getFavorites(String email);
    boolean isFavorite(Long bookId, String email);
}
