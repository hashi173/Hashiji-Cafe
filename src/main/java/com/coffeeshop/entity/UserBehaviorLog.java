package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "user_behavior_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_id")
    private java.util.UUID productId;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "action_weight", precision = 5, scale = 2)
    private BigDecimal actionWeight;
}
