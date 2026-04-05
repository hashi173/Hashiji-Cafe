package com.coffeeshop.repository;

import com.coffeeshop.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import com.coffeeshop.entity.Product;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    @Query("SELECT od.product FROM OrderDetail od WHERE od.product.active = true GROUP BY od.product ORDER BY SUM(od.quantity) DESC")
    List<Product> findTopSellingProducts(Pageable pageable);

    // Get products purchased by a specific user
    @Query("SELECT DISTINCT od.product FROM OrderDetail od JOIN od.order o WHERE o.user.id = :userId AND od.product.active = true")
    List<Product> findProductsPurchasedByUser(@org.springframework.data.repository.query.Param("userId") Long userId);

    // Get all user purchases mappings for KNN
    @Query("SELECT DISTINCT o.user.id, od.product.id FROM OrderDetail od JOIN od.order o WHERE o.user IS NOT NULL")
    List<Object[]> findAllUserPurchases();
}
