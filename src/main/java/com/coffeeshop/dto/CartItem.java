package com.coffeeshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long productId;
    private String productName;
    private String productNameVi;
    private String productImage;
    private Long sizeId;
    private String sizeName;
    private Double price; // Unit price (Base + Size + Toppings)
    private Integer quantity;
    private List<Long> toppingIds = new ArrayList<>();
    private List<String> toppingNames = new ArrayList<>();
    private java.util.Map<String, String> attributes = new java.util.HashMap<>();
    private String note;

    public Double getTotal() {
        return price * quantity;
    }
}
