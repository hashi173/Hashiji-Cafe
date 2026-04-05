package com.coffeeshop;

import com.coffeeshop.dto.PosOrderItemDto;
import com.coffeeshop.entity.*;
import com.coffeeshop.repository.*;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.WorkShiftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InventoryIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private ProductRecipeRepository productRecipeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkShiftService workShiftService;
    // Removed RoleRepository as Role is an Enum

    private User staffUser;
    private Product latte;
    private Ingredient milk;

    @BeforeEach
    void setUp() {
        // Create User
        staffUser = new User();
        staffUser.setUsername("teststaff");
        staffUser.setPassword("password");
        staffUser.setEmail("staff@test.com");
        staffUser.setRole(com.coffeeshop.entity.Role.ADMIN); // Use Enum
        userRepository.save(staffUser);

        // Start Shift
        workShiftService.startShift(staffUser, 1000.0);

        // Create Ingredient
        milk = new Ingredient();
        milk.setName("Milk");
        milk.setUnit("ml");
        milk.setStockQuantity(100.0); // Initial 100ml
        milk.setCostPerUnit(10.0);
        ingredientRepository.save(milk);

        // Create Product
        latte = new Product();
        latte.setName("Latte");
        // Product has no price, relies on ProductSize or manual entry
        latte.setActive(true);
        productRepository.save(latte);

        // Create Recipe
        ProductRecipe recipe = new ProductRecipe();
        recipe.setProduct(latte);
        recipe.setIngredient(milk);
        recipe.setQuantityRequired(200.0); // Requires 200ml
        productRecipeRepository.save(recipe);
    }

    @Test
    void testInsufficientInventory() {
        // Milk stock is 100, requires 200
        PosOrderItemDto item = new PosOrderItemDto();
        item.setProductId(latte.getId());
        item.setProductName(latte.getName());
        item.setQuantity(1);
        item.setPrice(20000.0);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createPosOrder(Collections.singletonList(item), staffUser);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void testSufficientInventory() {
        // Increase Milk stock to 300
        milk.setStockQuantity(300.0);
        ingredientRepository.save(milk);

        PosOrderItemDto item = new PosOrderItemDto();
        item.setProductId(latte.getId());
        item.setProductName(latte.getName());
        item.setQuantity(1);
        item.setPrice(20000.0);

        Order order = orderService.createPosOrder(Collections.singletonList(item), staffUser);

        assertNotNull(order.getId());

        // Verify stock deduction (300 - 200 = 100)
        Ingredient updatedMilk = ingredientRepository.findById(milk.getId()).orElseThrow();
        assertEquals(100.0, updatedMilk.getStockQuantity());
    }
}
