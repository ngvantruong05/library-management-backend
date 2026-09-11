package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.CategoryDto;
import edu.uet.library_management.domain.model.Category;
import edu.uet.library_management.domain.service.CategoryService;
import edu.uet.library_management.infrastructure.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Long> countsMap = loadCategoryBookCounts();
        return categories.stream()
                .map(c -> toDto(c, countsMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
        Long count = categoryRepository.countBooksByCategoryId(id);
        return toDto(category, count != null ? count : 0L);
    }

    @Override
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category already exists with name: " + categoryDto.getName());
        }
        Category category = Category.builder()
                .name(categoryDto.getName())
                .build();
        Category savedCategory = categoryRepository.save(category);
        return toDto(savedCategory, 0L);
    }

    @Override
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(categoryDto.getName()) && categoryRepository.existsByName(categoryDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category already exists with name: " + categoryDto.getName());
        }

        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);
        Long count = categoryRepository.countBooksByCategoryId(id);
        return toDto(updatedCategory, count != null ? count : 0L);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    private Map<Long, Long> loadCategoryBookCounts() {
        List<Object[]> rows = categoryRepository.countBooksPerCategory();
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Long catId = ((Number) row[0]).longValue();
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                map.put(catId, count);
            }
        }
        return map;
    }

    private CategoryDto toDto(Category category, Long bookCount) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .bookCount(bookCount)
                .build();
    }
}
