package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * Represents an item within an Order.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "snapshot_product_name", length = 255)
    private String snapshotProductName;

    @Column(name = "snapshot_unit_price", precision = 12, scale = 2)
    private BigDecimal snapshotUnitPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "snapshot_options", columnDefinition = "JSONB")
    private String snapshotOptions;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    // ─── Backward-compatibility aliases for Thymeleaf templates ─────

    /** Alias: templates reference detail.productName */
    @Transient
    public String getProductName() {
        return snapshotProductName;
    }

    /** Alias: templates reference detail.sizeSelected */
    @Transient
    public String getSizeSelected() {
        return snapshotOptions;
    }

    /** Alias: templates reference detail.unitPrice */
    @Transient
    public BigDecimal getUnitPrice() {
        return snapshotUnitPrice;
    }
}
