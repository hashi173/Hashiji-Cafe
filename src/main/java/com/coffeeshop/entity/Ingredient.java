package com.coffeeshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String unit; // e.g., "g", "ml", "pcs"

    @Column(name = "stock_quantity", nullable = false)
    private Double stockQuantity = 0.0;

    @Column(name = "cost_per_unit")
    private Double costPerUnit = 0.0;
}
