package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Details of a specific item in an Order.
 * Snapshots the product name and price at the time of purchase
 * so future price changes don't affect old orders.
 */
@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Snapshot data
    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "size_selected", length = 50)
    private String sizeSelected; // S, M, L

    @Column(name = "attributes", length = 500)
    private String attributes; // JSON or Text: "Sugar: 50%, Ice: None"

    @Column(name = "price_at_purchase")
    private Double priceAtPurchase; // Including toppings

    private Integer quantity;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetailTopping> selectedToppings;
}
