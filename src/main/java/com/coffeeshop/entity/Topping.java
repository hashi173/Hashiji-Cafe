package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents Add-ons like Pearls, Jelly, Extra Shot.
 * Has a fixed price.
 */
@Entity
@Table(name = "toppings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topping extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;
    private Double price;
}
