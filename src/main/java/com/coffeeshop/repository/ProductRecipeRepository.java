package com.coffeeshop.repository;

import com.coffeeshop.entity.ProductRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
}
