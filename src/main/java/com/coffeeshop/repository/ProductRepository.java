package com.coffeeshop.repository;

import com.coffeeshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
        List<Product> findByCategoryId(Long categoryId);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes WHERE p.category.id = :categoryId AND p.active = true")
        List<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId);

        List<Product> findByActiveTrue();

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes WHERE p.active = true")
        List<Product> findAllWithDetails();

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes")
        List<Product> findAllWithDetailsAll();

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Product> searchProducts(String keyword);

        @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes", countQuery = "SELECT count(p) FROM Product p")
        org.springframework.data.domain.Page<Product> findAllWithDetailsPaginated(
                        org.springframework.data.domain.Pageable pageable);

        @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))", countQuery = "SELECT count(p) FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        org.springframework.data.domain.Page<Product> searchProductsPaginated(@Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);

        @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes WHERE p.active = :active", countQuery = "SELECT count(p) FROM Product p WHERE p.active = :active")
        org.springframework.data.domain.Page<Product> findByActiveWithDetailsPaginated(@Param("active") boolean active,
                        org.springframework.data.domain.Pageable pageable);
}
