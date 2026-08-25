package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.CommentCreateRequest;
import edu.uet.library_management.domain.dto.CommentDto;
import edu.uet.library_management.domain.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<CommentDto>> getBookComments(@PathVariable Long bookId) {
        return ResponseEntity.ok(commentService.getCommentsByBook(bookId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(commentService.addComment(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long id,
            @RequestBody CommentCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(commentService.updateComment(id, request.getContent(), authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            Authentication authentication) {
        commentService.deleteComment(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
