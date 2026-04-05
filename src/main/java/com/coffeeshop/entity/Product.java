package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing a menu item (beverage or food).
 * Prices are determined by {@link ProductSize}, not stored directly on Product.
 * The {@code tags} field provides comma-separated keywords for AI semantic search
 * (e.g., "milk,sữa,cream,sweet" enables matching when a user types "sữa").
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    // --- Relationships ---

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductRecipe> recipes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSize> sizes;

    // --- Basic Fields ---

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String nameVi;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String descriptionVi;

    /** Comma-separated search keywords for AI recommendation matching (EN + VI). */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 500)
    private String image;

    private boolean active = true;
}
