package com.coffeeshop.repository;

import com.coffeeshop.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, java.util.UUID> {
}
