package com.coffeeshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Product Category (e.g., Coffee, Tea, Smoothie).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @jakarta.persistence.Column(unique = true, nullable = false, length = 50)
    private String name;

    @jakarta.persistence.Column(length = 50)
    private String nameVi;

    @jakarta.persistence.Column(length = 255)
    private String description;
}
