package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a User in the system.
 * Can be a Customer, Staff, or Admin.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // Password will be encrypted using BCrypt
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "user_code", unique = true, length = 20)
    private String userCode; // e.g., "S001"

    @Column(nullable = false)
    private boolean active = true;
}
