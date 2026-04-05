package com.coffeeshop.service;

import com.coffeeshop.entity.Product;
import com.coffeeshop.repository.OrderDetailRepository;
import com.coffeeshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered product recommendation engine.
 * Supports five strategies: Cold Start, Content-Based Filtering (TF Cosine Similarity),
 * Collaborative Filtering (User-based KNN with Jaccard), Hybrid, and Semantic Search.
 *
 * The {@code Product.tags} field is critical for accurate semantic search. Tags should
 * contain comma-separated keywords in both English and Vietnamese so that a query like
 * "sữa" correctly matches milk-based products.
 */
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    // ========================
    // 1. Cold Start: Best Sellers
    // ========================

    /**
     * Fallback strategy for anonymous users with no browsing context.
     * Returns the top-selling products by total quantity ordered.
     */
    public List<Product> getBestSellers(int limit) {
        return orderDetailRepository.findTopSellingProducts(PageRequest.of(0, limit));
    }

    // ========================
    // 2. Content-Based Filtering
    // ========================

    /**
     * Recommends products similar to the given product using TF-based Cosine Similarity
     * on concatenated text fields: category name + product name + description + tags.
     */
    public List<Product> getContentBasedRecommendations(Long currentProductId, int limit) {
        Optional<Product> optionalProduct = productRepository.findById(currentProductId);
        if (optionalProduct.isEmpty()) return getBestSellers(limit);

        Product currentProduct = optionalProduct.get();
        List<Product> allProducts = productRepository.findByActiveTrue();
        allProducts.removeIf(p -> p.getId().equals(currentProductId));

        Map<Long, Double> similarities = new HashMap<>();
        Map<Long, Product> productMap = new HashMap<>();

        for (Product p : allProducts) {
            similarities.put(p.getId(), calculateCosineSimilarity(currentProduct, p));
            productMap.put(p.getId(), p);
        }

        return similarities.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .map(e -> productMap.get(e.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Builds a combined text representation of a product for similarity computation.
     * Tags are repeated to give them higher weight in the term-frequency vector.
     */
    private String buildProductText(Product p) {
        StringBuilder sb = new StringBuilder();
        if (p.getCategory() != null) sb.append(p.getCategory().getName()).append(" ");
        if (p.getName() != null) sb.append(p.getName()).append(" ");
        if (p.getNameVi() != null) sb.append(p.getNameVi()).append(" ");
        if (p.getDescription() != null) sb.append(p.getDescription()).append(" ");
        if (p.getDescriptionVi() != null) sb.append(p.getDescriptionVi()).append(" ");
        // Tags get triple weight for precise matching
        if (p.getTags() != null) {
            String tags = p.getTags().replace(",", " ");
            sb.append(tags).append(" ").append(tags).append(" ").append(tags);
        }
        return sb.toString().toLowerCase();
    }

    /** Computes cosine similarity between two products based on term-frequency vectors. */
    private double calculateCosineSimilarity(Product p1, Product p2) {
        Map<String, Integer> vector1 = getTermFrequency(buildProductText(p1));
        Map<String, Integer> vector2 = getTermFrequency(buildProductText(p2));

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vector1.keySet());
        allTerms.addAll(vector2.keySet());

        double dotProduct = 0, norm1 = 0, norm2 = 0;
        for (String term : allTerms) {
            int val1 = vector1.getOrDefault(term, 0);
            int val2 = vector2.getOrDefault(term, 0);
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /** Tokenizes text and returns a word-frequency map. Words shorter than 2 chars are excluded. */
    private Map<String, Integer> getTermFrequency(String text) {
        Map<String, Integer> tf = new HashMap<>();
        if (text == null || text.isBlank()) return tf;

        for (String word : text.split("\\W+")) {
            if (word.length() > 1) {
                tf.merge(word, 1, Integer::sum);
            }
        }
        return tf;
    }

    // ========================
    // 3. Collaborative Filtering (User-based KNN)
    // ========================

    /**
     * Recommends products purchased by users with similar order history.
     * Uses weighted Jaccard similarity to find K nearest neighbors.
     */
    public List<Product> getCollaborativeRecommendations(Long userId, int limit) {
        List<Product> userHistory = orderDetailRepository.findProductsPurchasedByUser(userId);
        if (userHistory.isEmpty()) return getBestSellers(limit);

        Set<Long> myProductIds = userHistory.stream().map(Product::getId).collect(Collectors.toSet());
        List<Object[]> allPurchases = orderDetailRepository.findAllUserPurchases();

        // Build user -> purchased product set
        Map<Long, Set<Long>> userToProducts = new HashMap<>();
        for (Object[] row : allPurchases) {
            Long uid = ((Number) row[0]).longValue();
            Long pid = ((Number) row[1]).longValue();
            userToProducts.computeIfAbsent(uid, k -> new HashSet<>()).add(pid);
        }
        userToProducts.remove(userId);

        // Weighted Jaccard: jaccard * overlap_count to penalize small-overlap users
        Map<Long, Double> userSimilarities = new HashMap<>();
        for (var entry : userToProducts.entrySet()) {
            Set<Long> otherProducts = entry.getValue();
            Set<Long> intersection = new HashSet<>(myProductIds);
            intersection.retainAll(otherProducts);
            Set<Long> union = new HashSet<>(myProductIds);
            union.addAll(otherProducts);
            double jaccard = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
            userSimilarities.put(entry.getKey(), jaccard * intersection.size());
        }

        // Top-K nearest users
        int K = 5;
        List<Long> topKUsers = userSimilarities.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(K)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Aggregate product scores from neighbors
        Map<Long, Double> productScores = new HashMap<>();
        for (Long similarUserId : topKUsers) {
            double simScore = userSimilarities.get(similarUserId);
            for (Long pid : userToProducts.get(similarUserId)) {
                if (!myProductIds.contains(pid)) {
                    productScores.merge(pid, simScore, Double::sum);
                }
            }
        }

        List<Long> recommendedIds = productScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendedIds.isEmpty()) return getBestSellers(limit);
        return productRepository.findAllById(recommendedIds);
    }

    // ========================
    // 4. Hybrid (Content + Collaborative)
    // ========================

    /**
     * Blends Content-Based and Collaborative scores with 40/60 weighting.
     * Falls back gracefully if either signal is unavailable.
     */
    public List<Product> getHybridRecommendations(Long userId, Long currentProductId, int limit) {
        if (userId == null && currentProductId == null) return getBestSellers(limit);

        int poolSize = limit * 2;
        List<Product> contentRecs = currentProductId != null
                ? getContentBasedRecommendations(currentProductId, poolSize) : List.of();
        List<Product> collabRecs = userId != null
                ? getCollaborativeRecommendations(userId, poolSize) : List.of();

        if (contentRecs.isEmpty()) return collabRecs.isEmpty()
                ? getBestSellers(limit) : collabRecs.stream().limit(limit).toList();
        if (collabRecs.isEmpty()) return contentRecs.stream().limit(limit).toList();

        // Weighted rank-fusion
        Map<Long, Double> finalScores = new HashMap<>();
        Map<Long, Product> productMap = new HashMap<>();

        for (int i = 0; i < contentRecs.size(); i++) {
            Product p = contentRecs.get(i);
            finalScores.merge(p.getId(), (poolSize - i) * 0.4, Double::sum);
            productMap.putIfAbsent(p.getId(), p);
        }
        for (int i = 0; i < collabRecs.size(); i++) {
            Product p = collabRecs.get(i);
            finalScores.merge(p.getId(), (poolSize - i) * 0.6, Double::sum);
            productMap.putIfAbsent(p.getId(), p);
        }

        return finalScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> productMap.get(e.getKey()))
                .collect(Collectors.toList());
    }

    // ========================
    // 5. Semantic Search (NLP Query Match)
    // ========================

    /**
     * Matches user free-text query against all active products.
     * Tags are weighted 3x to ensure ingredient-level queries (e.g., "sữa")
     * correctly prioritize relevant products over incidental name matches.
     */
    public List<Product> getRecommendationsByQuery(String query, int limit) {
        if (query == null || query.isBlank()) return getBestSellers(limit);

        List<Product> allProducts = productRepository.findByActiveTrue();
        Map<String, Integer> queryVector = getTermFrequency(query.toLowerCase());

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Product> productMap = new HashMap<>();

        for (Product p : allProducts) {
            String text = buildProductText(p);
            Map<String, Integer> pVector = getTermFrequency(text);

            double score = 0;
            for (String qTerm : queryVector.keySet()) {
                score += pVector.getOrDefault(qTerm, 0) * 1.5;
            }

            if (score > 0) {
                scores.put(p.getId(), score);
                productMap.put(p.getId(), p);
            }
        }

        if (scores.isEmpty()) return getBestSellers(limit);

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> productMap.get(e.getKey()))
                .collect(Collectors.toList());
    }
}
