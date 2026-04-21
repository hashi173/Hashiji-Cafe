package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a Customer Order.
 * Merges the PDF schema (UUID, UserAddress, Promotion, grandTotal, orderStatus)
 * with the legacy fields (customerName, phone, trackingCode, etc.) required
 * by the existing Thymeleaf UI and controllers.
 */
@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so we use "orders"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    // ─── PDF Schema Fields ───────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private UserAddress address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "order_status", length = 50)
    private String orderStatus; // PENDING, COMPLETED, etc. (String version for PDF schema)

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // COD, VNPay, etc.

    @Column(name = "payment_status", length = 50)
    private String paymentStatus; // UNPAID, PAID, etc.

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    // ─── Legacy Fields (for existing UI / Controllers) ──────────────

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address_text", columnDefinition = "TEXT")
    private String addressText; // free-form address string (legacy)

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "tracking_code", unique = true)
    private String trackingCode;

    @Column(name = "order_type")
    private String orderType;

    // ─── Convenience Accessors ──────────────────────────────────────

    /** Alias for legacy code that calls getOrderDetails() */
    @Transient
    public List<OrderItem> getOrderDetails() {
        return orderItems;
    }

    /** Legacy setter: maps "address" string to "addressText" to avoid clash with UserAddress field */
    public void setAddress(String address) {
        this.addressText = address;
    }

    /** Legacy getter: maps "address" string from "addressText" */
    public String getAddress() {
        return this.addressText;
    }
}
