package com.coffeeshop.service;

import com.coffeeshop.entity.Category;
import com.coffeeshop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @org.springframework.cache.annotation.Cacheable("categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public java.util.Optional<Category> getCategoryById(@org.springframework.lang.NonNull Long id) {
        return categoryRepository.findById(id);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public Category saveCategory(@org.springframework.lang.NonNull Category category) {
        return categoryRepository.save(category);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(@org.springframework.lang.NonNull Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword);
    }

    public org.springframework.data.domain.Page<Category> getAllCategoriesPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<Category> searchCategoriesPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

}
