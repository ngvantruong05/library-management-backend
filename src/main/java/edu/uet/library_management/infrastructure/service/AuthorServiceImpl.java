package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.AuthorDto;
import edu.uet.library_management.domain.model.Author;
import edu.uet.library_management.domain.service.AuthorService;
import edu.uet.library_management.infrastructure.persistence.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public List<AuthorDto> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AuthorDto getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + id));
        return toDto(author);
    }

    @Override
    public AuthorDto createAuthor(AuthorDto authorDto) {
        if (authorRepository.existsByName(authorDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Author already exists with name: " + authorDto.getName());
        }
        Author author = Author.builder()
                .name(authorDto.getName())
                .description(authorDto.getDescription())
                .build();
        Author savedAuthor = authorRepository.save(author);
        return toDto(savedAuthor);
    }

    @Override
    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + id));

        if (!author.getName().equalsIgnoreCase(authorDto.getName()) && authorRepository.existsByName(authorDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Author already exists with name: " + authorDto.getName());
        }

        author.setName(authorDto.getName());
        author.setDescription(authorDto.getDescription());
        Author updatedAuthor = authorRepository.save(author);
        return toDto(updatedAuthor);
    }

    @Override
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + id));
        authorRepository.delete(author);
    }

    private AuthorDto toDto(Author author) {
        return AuthorDto.builder()
                .id(author.getId())
                .name(author.getName())
                .description(author.getDescription())
                .build();
    }
}
