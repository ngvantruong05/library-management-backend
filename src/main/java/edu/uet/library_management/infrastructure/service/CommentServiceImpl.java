package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.CommentCreateRequest;
import edu.uet.library_management.domain.dto.CommentDto;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.Comment;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.domain.service.CommentService;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.CommentRepository;
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
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public List<CommentDto> getCommentsByBook(Long bookId) {
        return commentRepository.findByBookIdOrderByCreatedAtAsc(bookId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(CommentCreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        Comment comment = Comment.builder()
                .user(user)
                .book(book)
                .content(request.getContent())
                .build();

        return toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long commentId, String content, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot edit someone else's comment");
        }

        comment.setContent(content);
        return toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        User actor = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!comment.getUser().getEmail().equals(userEmail) && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .bookId(comment.getBook().getId())
                .userId(comment.getUser().getId())
                .userDisplayName(comment.getUser().getDisplayName() != null ? comment.getUser().getDisplayName() : comment.getUser().getEmail())
                .userEmail(comment.getUser().getEmail())
                .userPhotoUrl(comment.getUser().getPhotoUrl())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
