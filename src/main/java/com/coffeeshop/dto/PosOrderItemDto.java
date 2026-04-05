package com.coffeeshop.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosOrderItemDto {
    private Long productId;
    private String productName;
    private String sizeName;
    private Double price;
    private Integer quantity;
    private List<String> toppingNames;
    private java.util.Map<String, String> attributes;
}
