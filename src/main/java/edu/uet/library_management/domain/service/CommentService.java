package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.CommentCreateRequest;
import edu.uet.library_management.domain.dto.CommentDto;
import java.util.List;

public interface CommentService {
    List<CommentDto> getCommentsByBook(Long bookId);
    CommentDto addComment(CommentCreateRequest request, String userEmail);
    CommentDto updateComment(Long commentId, String content, String userEmail);
    void deleteComment(Long commentId, String userEmail);
}
