package com.coffeeshop;

import com.coffeeshop.dto.PosOrderItemDto;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.Role;
import com.coffeeshop.entity.User;
import com.coffeeshop.repository.ProductRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.WorkShiftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShiftIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WorkShiftService workShiftService;
    @Autowired
    private UserRepository userRepository;
    // Removed RoleRepository
    @Autowired
    private ProductRepository productRepository;

    private User staffUser;
    private Product product;

    @BeforeEach
    void setUp() {
        // Create User
        staffUser = new User();
        staffUser.setUsername("shiftstaff");
        staffUser.setPassword("password");
        staffUser.setEmail("shift@test.com");
        staffUser.setRole(com.coffeeshop.entity.Role.STAFF); // Use Enum
        userRepository.save(staffUser);

        // Create dummy product
        product = new Product();
        product.setName("Coffee");
        // product.setPrice(20000); // Product has no price field
        productRepository.save(product);
    }

    @Test
    void testOrderWithNoShift() {
        PosOrderItemDto item = new PosOrderItemDto();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(1);
        item.setPrice(20000.0); // Product has no price, use hardcoded for test

        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.createPosOrder(Collections.singletonList(item), staffUser);
        });

        assertTrue(exception.getMessage().contains("No active shift found"));
    }

    @Test
    void testOrderWithActiveShift() {
        workShiftService.startShift(staffUser, 500.0);

        PosOrderItemDto item = new PosOrderItemDto();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(1);
        item.setPrice(20000.0);

        Order order = orderService.createPosOrder(Collections.singletonList(item), staffUser);

        assertNotNull(order);
        assertNotNull(order.getId());
    }
}
