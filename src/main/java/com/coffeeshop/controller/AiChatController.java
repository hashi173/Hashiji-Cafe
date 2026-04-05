package com.coffeeshop.controller;

import com.coffeeshop.entity.Product;
import com.coffeeshop.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for AI-powered product recommendations.
 * Selects the best recommendation strategy based on available context:
 * query text, current product, and/or authenticated user.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiRecommendationService aiRecommendationService;

    /**
     * Returns product recommendations based on the available context.
     *
     * @param productId currently viewed product (optional)
     * @param query     free-text search query (optional)
     * @param auth      Spring Security authentication (auto-injected)
     * @return JSON with strategy name and list of recommended products
     */
    @GetMapping("/recommend")
    public Map<String, Object> recommend(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "query", required = false) String query,
            Authentication auth) {

        Long userId = extractUserId(auth);

        List<Product> recommendations;
        String strategy;

        if (query != null && !query.isBlank()) {
            // User typed a question -> semantic search
            recommendations = aiRecommendationService.getRecommendationsByQuery(query, 5);
            strategy = "Semantic Match (NLP)";
        } else if (userId == null && productId == null) {
            // Anonymous user, no product context -> best sellers
            recommendations = aiRecommendationService.getBestSellers(5);
            strategy = "Cold Start (Best Sellers)";
        } else if (userId == null) {
            // Anonymous but viewing a product -> content-based
            recommendations = aiRecommendationService.getContentBasedRecommendations(productId, 5);
            strategy = "Content-Based Filtering (Cosine Similarity)";
        } else if (productId == null) {
            // Logged in but no product context -> collaborative
            recommendations = aiRecommendationService.getCollaborativeRecommendations(userId, 5);
            strategy = "Collaborative Filtering (KNN)";
        } else {
            // Both signals available -> hybrid
            recommendations = aiRecommendationService.getHybridRecommendations(userId, productId, 5);
            strategy = "Hybrid Recommendation (Collab + Content)";
        }

        // Map to safe DTO to prevent JSON recursion and data leakage
        List<Map<String, Object>> resultList = recommendations.stream().map(p -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", p.getId());
            dto.put("name", p.getName());
            dto.put("image", p.getImage() != null ? p.getImage() : "https://placehold.co/150");
            dto.put("price", !p.getSizes().isEmpty() ? p.getSizes().get(0).getPrice() : 0);
            return dto;
        }).collect(Collectors.toList());

        return Map.of("strategy", strategy, "recommendations", resultList);
    }

    /** Safely extracts user ID from authentication principal via reflection. */
    private Long extractUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            Object principal = auth.getPrincipal();
            return (Long) principal.getClass().getMethod("getId").invoke(principal);
        } catch (Exception e) {
            return null;
        }
    }
}
