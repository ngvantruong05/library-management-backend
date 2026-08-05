package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.BookDto;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.Favorite;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.domain.service.BookService;
import edu.uet.library_management.domain.service.FavoriteService;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.FavoriteRepository;
import edu.uet.library_management.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    @Override
    @Transactional
    public void addFavorite(Long bookId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));

        if (favoriteRepository.existsByUserEmailAndBookId(email, bookId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book is already in favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .book(book)
                .build();

        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long bookId, String email) {
        if (!favoriteRepository.existsByUserEmailAndBookId(email, bookId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book is not in favorites");
        }
        favoriteRepository.deleteByUserEmailAndBookId(email, bookId);
    }

    @Override
    public List<BookDto> getFavorites(String email) {
        return favoriteRepository.findByUserEmail(email).stream()
                .map(fav -> bookService.getBookById(fav.getBook().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFavorite(Long bookId, String email) {
        return favoriteRepository.existsByUserEmailAndBookId(email, bookId);
    }
}
