package com.coffeeshop.service;

import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductSize;
import com.coffeeshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

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

    public List<Product> searchProductsForMenu(String keyword, Long categoryId) {
        return filterProductsFuzzy(getProductsByCategory(categoryId), keyword);
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
    @org.springframework.cache.annotation.Cacheable(value = "products", key = "'search_' + #keyword")
    public List<Product> searchProductsFuzzy(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAllProducts();
        return filterProductsFuzzy(getAllProducts(), keyword);
    }

    private List<Product> filterProductsFuzzy(List<Product> products, String keyword) {
        if (keyword == null || keyword.isBlank()) return products;

        String[] queryTerms = normalizeSearchText(keyword).split("\\W+");

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Product> productMap = new HashMap<>();

        for (Product p : products) {
            double productScore = 0;

            String name = normalizeSearchText(p.getName());
            String nameVi = normalizeSearchText(p.getNameVi());
            String tags = normalizeSearchText(p.getTags() != null ? p.getTags().replace(",", " ") : "");
            String desc = normalizeSearchText(p.getDescription());
            String descVi = normalizeSearchText(p.getDescriptionVi());

            for (String qTerm : queryTerms) {
                if (qTerm.length() < 2) continue;

                // 1. Name Match (Weighted High)
                if (name.contains(qTerm) || nameVi.contains(qTerm)) {
                    productScore += 100.0;
                    if (name.equals(qTerm) || nameVi.equals(qTerm)) productScore += 50.0;
                } else if (isFuzzyMatch(qTerm, name) || isFuzzyMatch(qTerm, nameVi)) {
                    productScore += 40.0;
                }

                // 2. Tag Match (Weighted Medium-High)
                if (tags.contains(qTerm)) {
                    productScore += 60.0;
                } else if (isFuzzyMatch(qTerm, tags)) {
                    productScore += 30.0;
                }
                
                // 3. Description Match (Weighted Low as requested)
                if (desc.contains(qTerm) || descVi.contains(qTerm)) {
                    productScore += 5.0;
                }
            }

            if (productScore > 0) {
                scores.put(p.getId(), productScore);
                productMap.put(p.getId(), p);
            }
        }

        if (scores.isEmpty()) return List.of();

        // 40% Significance Threshold
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double threshold = maxScore * 0.4;

        return scores.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .sorted((a, b) -> Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue()))
                .map(e -> productMap.get(e.getKey()))
                .collect(Collectors.toList());
    }

    private String normalizeSearchText(String text) {
        if (text == null) return "";

        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase()
                .trim();
    }

    private boolean isFuzzyMatch(String queryTerm, String targetText) {
        if (targetText == null || targetText.isEmpty()) return false;
        if (queryTerm.length() <= 3) return false; // "trà" must match exactly in contains()

        String[] targetTokens = targetText.split("\\W+");
        for (String token : targetTokens) {
            if (token.length() < 3) continue;
            int distance = calculateLevenshteinDistance(queryTerm, token);
            int maxDistance = (queryTerm.length() > 6) ? 2 : 1;
            if (distance <= maxDistance) return true;
        }
        return false;
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
