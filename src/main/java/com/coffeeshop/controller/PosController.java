package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductSize;
import com.coffeeshop.entity.Topping;
import com.coffeeshop.service.CategoryService;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.ProductService;
import com.coffeeshop.service.ToppingService;
import com.coffeeshop.service.UserService;
import com.coffeeshop.service.WorkShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ToppingService toppingService;
    private final OrderService orderService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final WorkShiftService workShiftService;
    private final UserService userService;

    @GetMapping
    public String posView(Model model, java.security.Principal principal)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        // Get Current User
        com.coffeeshop.entity.User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check for Open Shift
        java.util.Optional<com.coffeeshop.entity.WorkShift> currentShift = workShiftService
                .getCurrentShift(currentUser);
        if (currentShift.isEmpty()) {
            return "pos/start-shift";
        }

        model.addAttribute("currentShift", currentShift.get());
        model.addAttribute("categories", categoryService.getAllCategories());

        // Prepare Products for JSON (Simplified)
        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product p : productService.getAllProducts()) {
            List<Map<String, Object>> sizes = new ArrayList<>();
            if (p.getSizes() != null) {
                for (ProductSize s : p.getSizes()) {
                    sizes.add(Map.of(
                            "id", s.getId(),
                            "sizeName", s.getSizeName(),
                            "price", s.getPrice()));
                }
            }

            productList.add(Map.of(
                    "id", p.getId(),
                    "name", p.getName(),
                    "nameVi", p.getNameVi() != null ? p.getNameVi() : "",
                    "image", p.getImage() != null ? p.getImage() : "",
                    "categoryId", p.getCategory() != null ? p.getCategory().getId() : 0,
                    "sizes", sizes));
        }
        model.addAttribute("productsJson", objectMapper.writeValueAsString(productList));
        model.addAttribute("products", productService.getAllProducts()); // Keep for Thymeleaf HTML loop if needed, or
                                                                         // remove if fully JS

        // Prepare Toppings for JSON
        List<Map<String, Object>> toppingList = new ArrayList<>();
        for (Topping t : toppingService.getAllToppings()) {
            toppingList.add(Map.of(
                    "id", t.getId(),
                    "name", t.getName(),
                    "price", t.getPrice()));
        }
        model.addAttribute("toppingsJson", objectMapper.writeValueAsString(toppingList));

        // Online/Active Orders
        model.addAttribute("pendingOrders", orderService.getActiveOrders()); // Using name 'pendingOrders' for backward
                                                                             // compat in JS, or rename?
        // Let's rename key in HTML to 'activeOrders' but if I change it here, I must
        // update HTML in same turn or next.
        // For minimal breakage, I'll pass it as 'activeOrders' AND 'pendingOrders' or
        // just update HTML.
        // Let's update HTML active orders.
        model.addAttribute("activeOrders", orderService.getActiveOrders());
        model.addAttribute("orderStatuses", com.coffeeshop.entity.OrderStatus.values());

        return "pos/index";
    }

    @PostMapping("/shift/start")
    public String startShift(@RequestParam Double startCash, java.security.Principal principal) {
        com.coffeeshop.entity.User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        workShiftService.startShift(currentUser, startCash);
        return "redirect:/pos";
    }

    @PostMapping("/shift/end")
    public String endShift(@RequestParam Double endCash, java.security.Principal principal) {
        com.coffeeshop.entity.User currentUser = userService.findByUsername(principal.getName())

                .orElseThrow(() -> new RuntimeException("User not found"));
        workShiftService.endShift(currentUser, endCash);
        return "redirect:/logout"; // Logout after closing shift
    }

    // JSON API for handling POS actions (Add to internal session cart or direct
    // DB?)
    // For better experience, let's use a specific API for submitting the POS order

    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody POSOrderRequest request, java.security.Principal principal) {
        // Map Request to DTOs
        List<com.coffeeshop.dto.PosOrderItemDto> dtos = new ArrayList<>();

        if (request.getItems() != null) {
            for (POSOrderItem item : request.getItems()) {
                com.coffeeshop.dto.PosOrderItemDto dto = new com.coffeeshop.dto.PosOrderItemDto();
                dto.setProductId(item.getProductId());
                dto.setProductName(item.getProductName());
                dto.setSizeName(item.getSizeName());
                dto.setPrice(item.getPrice());
                dto.setQuantity(item.getQuantity());
                dto.setToppingNames(item.getToppingNames());
                dtos.add(dto);
            }
        }

        try {
            com.coffeeshop.entity.User currentUser = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Delegate to Service without using Cart logic
            Order order = orderService.createPosOrder(dtos, currentUser);
            return ResponseEntity.ok(Map.of("success", true, "orderId", order.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
            @RequestParam("status") com.coffeeshop.entity.OrderStatus status) {
        try {
            orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(Map.of("success", true, "message", "Status updated to " + status));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DTOs for JSON Body
    @lombok.Data
    static class POSOrderRequest {
        private List<POSOrderItem> items;
    }

    @lombok.Data
    static class POSOrderItem {
        private Long productId;
        private String productName;
        private String sizeName;
        private Double price;
        private Integer quantity;
        private List<String> toppingNames;
    }
}
