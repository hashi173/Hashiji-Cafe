package com.coffeeshop.repository;

import com.coffeeshop.entity.OrderDetailTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailToppingRepository extends JpaRepository<OrderDetailTopping, Long> {
}
