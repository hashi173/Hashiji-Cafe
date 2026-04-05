package com.coffeeshop.service;

import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductSize;
import com.coffeeshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @org.springframework.cache.annotation.Cacheable("products")
    public List<Product> getAllProducts() {
        return productRepository.findAllWithDetails();
    }

    @org.springframework.cache.annotation.Cacheable(value = "products", key = "#categoryId != null ? #categoryId : 'all'")
    public List<Product> getProductsByCategory(Long categoryId) {
        if (categoryId == null) {
            return getAllProducts();
        }
        return productRepository.findByCategoryIdAndActiveTrue(categoryId);
    }

    public List<Product> getAllProductsAdmin() {
        return productRepository.findAllWithDetailsAll();
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAllWithDetailsAll();
        }
        return productRepository.searchProducts(keyword);
    }

    public org.springframework.data.domain.Page<Product> getProductsPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return productRepository.findAllWithDetailsPaginated(pageable);
    }

    public org.springframework.data.domain.Page<Product> searchProductsPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return productRepository.searchProductsPaginated(keyword, pageable);
    }

    public org.springframework.data.domain.Page<Product> getProductsByStatusPaginated(boolean active,
            org.springframework.data.domain.Pageable pageable) {
        return productRepository.findByActiveWithDetailsPaginated(active, pageable);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "products", allEntries = true)
    public Product saveProduct(@org.springframework.lang.NonNull Product product) {
        // Ensure bidirectional relationship is set for Sizes
        if (product.getSizes() != null) {
            for (ProductSize size : product.getSizes()) {
                size.setProduct(product);
            }
        }
        return productRepository.save(product);
    }

    public void deleteProduct(@org.springframework.lang.NonNull Long id) {
        // Soft delete: just de-activate
        updateStatus(id, false);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "products", allEntries = true)
    public void updateStatus(Long id, boolean active) {
        productRepository.findById(id).ifPresent(product -> {
            product.setActive(active);
            productRepository.save(product);
        });
    }
}
