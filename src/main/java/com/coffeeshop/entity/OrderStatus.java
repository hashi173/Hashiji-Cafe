package com.coffeeshop.entity;

public enum OrderStatus {
    PENDING, // New order
    CONFIRMED, // Accepted by staff
    SHIPPING, // Out for delivery
    COMPLETED, // Done
    CANCELLED // Invalid
}
