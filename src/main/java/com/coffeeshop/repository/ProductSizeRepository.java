package com.coffeeshop.repository;

import com.coffeeshop.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, java.util.UUID> {
    java.util.List<ProductSize> findByProductId(java.util.UUID productId);
}
