package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Defines the Price for a specific Size of a Product.
 * Example:
 * Product: Black Coffee
 * - Size S: 20000
 * - Size M: 25000
 */
@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSize extends BaseEntity {

    // Size name: "S", "M", "L", or "Standard"
    @Column(name = "size_name")
    private String sizeName;

    private Double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
