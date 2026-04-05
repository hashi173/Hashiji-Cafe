package com.coffeeshop.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cart {
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem newItem) {
        // Check if similar item exists (same product, size, toppings)
        for (CartItem item : items) {
            if (isSameItem(item, newItem)) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                return;
            }
        }
        items.add(newItem);
    }

    public void updateQuantity(int index, int quantity) {
        if (index >= 0 && index < items.size()) {
            if (quantity <= 0) {
                items.remove(index);
            } else {
                items.get(index).setQuantity(quantity);
            }
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public Double getTotalAmount() {
        return items.stream().mapToDouble(CartItem::getTotal).sum();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    private boolean isSameItem(CartItem item1, CartItem item2) {
        // Customize equality check based on business needs
        // Simplest: same product and size. Ideally should check toppings too.
        boolean sameProduct = item1.getProductId().equals(item2.getProductId());
        boolean sameSize = item1.getSizeId().equals(item2.getSizeId());

        // Simple topping check (lists must be equal)
        boolean sameToppings = item1.getToppingIds().equals(item2.getToppingIds());

        // Attributes check
        boolean sameAttributes = item1.getAttributes().equals(item2.getAttributes());

        return sameProduct && sameSize && sameToppings && sameAttributes;
    }
}
