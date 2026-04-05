package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Represents a Customer Order.
 */
@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so we use "orders"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    // If customer is logged in, link to User. If guest, this might be null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Contact info (Snapshot at time of order)
    // Contact info (Snapshot at time of order)
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(length = 15)
    private String phone;

    @Column(length = 500)
    private String address;

    // Note from customer
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "total_amount")
    private Double totalAmount;

    // PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus status;

    @Column(name = "order_type", length = 50)
    private String orderType; // "Details", "POS Order", "Web Order"

    @Column(name = "tracking_code", unique = true, length = 50)
    private String trackingCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails;
}
