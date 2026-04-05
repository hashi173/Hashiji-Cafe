package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Stores which toppings were added to a specific drink.
 */
@Entity
@Table(name = "order_detail_toppings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailTopping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id")
    private Topping topping;

    @Column(name = "topping_name")
    private String toppingName;

    @Column(name = "price_at_purchase")
    private Double priceAtPurchase;
}
