package com.coffeeshop.entity;

/**
 * Defines the roles available in the system.
 * Simple Enum is used instead of a Table for simplicity and performance.
 */
public enum Role {
    ADMIN, // Full access
    STAFF, // POS and Order Management
    USER // Customer access
}
