package com.coffeeshop.repository;

import com.coffeeshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    java.util.Optional<Category> findByName(String name);

    java.util.List<Category> findByNameContainingIgnoreCase(String name);

    org.springframework.data.domain.Page<Category> findAll(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Category> findByNameContainingIgnoreCase(String name,
            org.springframework.data.domain.Pageable pageable);
}
