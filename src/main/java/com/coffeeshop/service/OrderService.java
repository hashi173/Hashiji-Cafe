package com.coffeeshop.service;

import com.coffeeshop.dto.Cart;
import com.coffeeshop.dto.CartItem;
import com.coffeeshop.entity.*;
import com.coffeeshop.repository.OrderDetailRepository;
import com.coffeeshop.repository.OrderDetailToppingRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.ToppingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderDetailToppingRepository orderDetailToppingRepository;
    private final ToppingRepository toppingRepository;
    private final com.coffeeshop.repository.ProductRepository productRepository;
    private final com.coffeeshop.repository.IngredientRepository ingredientRepository;
    private final WorkShiftService workShiftService;

    @Transactional
    public Order placeOrder(Cart cart, User user, String customerName, String phone, String address, String note) {
        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setNote(note);
        order.setTotalAmount(cart.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setTrackingCode(generateTrackingCode());

        Order savedOrder = orderRepository.save(order);

        for (CartItem item : cart.getItems()) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProductName(item.getProductName());
            detail.setSizeSelected(item.getSizeName());
            detail.setQuantity(item.getQuantity());
            detail.setPriceAtPurchase(item.getPrice());
            if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
                detail.setAttributes(item.getAttributes().toString());
            }

            if (item.getProductId() != null) {
                // Use getReferenceById to avoid extra SELECT if entity exists (lazy loading
                // proxy)
                // Or findById if we want to be safe. getReferenceById is standard for setting
                // FKs.
                try {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    if (product != null) {
                        detail.setProduct(product);
                        // Note: Inventory deduction happens when order status changes to COMPLETED
                    }
                } catch (Exception e) {
                    // Ignore if product not found
                }
            }

            OrderDetail savedDetail = orderDetailRepository.save(detail);

            if (item.getToppingIds() != null && !item.getToppingIds().isEmpty()) {
                @SuppressWarnings("null")
                List<Topping> toppings = toppingRepository.findAllById(item.getToppingIds());

                for (Topping topping : toppings) {
                    OrderDetailTopping detailTopping = new OrderDetailTopping();
                    detailTopping.setOrderDetail(savedDetail);
                    detailTopping.setTopping(topping);
                    detailTopping.setToppingName(topping.getName());
                    detailTopping.setPriceAtPurchase(topping.getPrice());

                    orderDetailToppingRepository.save(detailTopping);
                }
            }
        }

        return savedOrder;
    }

    /**
     * Dedicated method for POS orders to decouple from Web Cart logic.
     */
    @Transactional
    public Order createPosOrder(List<com.coffeeshop.dto.PosOrderItemDto> items, User staff) {
        // Enforce active shift
        workShiftService.getCurrentShift(staff)
                .orElseThrow(() -> new RuntimeException("No active shift found. Please start a shift first."));

        Order order = new Order();
        order.setUser(staff);
        order.setCustomerName("Walk-in Customer");
        order.setOrderType("POS Order");
        // Removed duplicate setOrderType line
        order.setStatus(OrderStatus.COMPLETED); // POS orders are immediate
        order.setTrackingCode(generateTrackingCode());
        order.setCreatedAt(java.time.LocalDateTime.now());

        double totalAmount = 0.0;
        Order savedOrder = orderRepository.save(order);

        for (com.coffeeshop.dto.PosOrderItemDto item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProductName(item.getProductName());
            detail.setSizeSelected(item.getSizeName());
            detail.setQuantity(item.getQuantity());
            detail.setPriceAtPurchase(item.getPrice());
            if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
                detail.setAttributes(item.getAttributes().toString());
            }

            if (item.getProductId() != null) {
                try {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    if (product != null) {
                        detail.setProduct(product);

                        // Inventory Logic
                        for (ProductRecipe recipe : product.getRecipes()) {
                            Ingredient ingredient = recipe.getIngredient();
                            double required = recipe.getQuantityRequired() * item.getQuantity();
                            if (ingredient.getStockQuantity() < required) {
                                throw new RuntimeException(
                                        "Insufficient stock: " + ingredient.getName() + " for " + product.getName());
                            }
                            ingredient.setStockQuantity(ingredient.getStockQuantity() - required);
                            ingredientRepository.save(ingredient);
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof RuntimeException)
                        throw e;
                }
            }

            // Add to total
            totalAmount += (item.getPrice() * item.getQuantity());

            orderDetailRepository.save(detail);

            // Note: For now, we are skipping topping details saving in the database for
            // manual POS items
            // to keep it simple as requested, and because we might not have Topping
            // entities for custom inputs.
            // If needed, we can add a text field "toppings" to OrderDetail.
        }

        savedOrder.setTotalAmount(totalAmount);
        return orderRepository.save(savedOrder);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        if (order != null) {
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(status);
            orderRepository.save(order);

            // Deduct inventory only when order is marked as COMPLETED (and wasn't already
            // COMPLETED)
            if (status == OrderStatus.COMPLETED && previousStatus != OrderStatus.COMPLETED) {
                deductInventoryForOrder(order);
            }
        }
    }

    /**
     * Deducts inventory based on order details and product recipes.
     */
    private void deductInventoryForOrder(Order order) {
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getProduct() != null) {
                Product product = detail.getProduct();
                for (ProductRecipe recipe : product.getRecipes()) {
                    Ingredient ingredient = recipe.getIngredient();
                    double required = recipe.getQuantityRequired() * detail.getQuantity();
                    if (ingredient.getStockQuantity() < required) {
                        throw new RuntimeException(
                                "Insufficient stock: " + ingredient.getName() + " for " + product.getName());
                    }
                    ingredient.setStockQuantity(ingredient.getStockQuantity() - required);
                    ingredientRepository.save(ingredient);
                }
            }
        }
    }

    public long countTotalOrders() {
        return orderRepository.count();
    }

    public long countPendingOrders() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }

    public List<Order> getPendingOrders() {
        return orderRepository.findAllByStatusOrderByCreatedAtDesc(OrderStatus.PENDING);
    }

    public List<Order> getActiveOrders() {
        return orderRepository.findAllByStatusInOrderByCreatedAtDesc(
                java.util.Arrays.asList(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING));
    }

    public double calculateTotalRevenue() {
        Double revenue = orderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED);
        return revenue != null ? revenue : 0.0;
    }

    private String generateTrackingCode() {
        // Example: ORD-X7A9-2B (Random alphanumeric)
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "ORD-" + uuid.substring(0, 6);
    }

    public List<Object[]> getDailyRevenue() {
        return orderRepository.findDailyRevenue();
    }

    public List<Object[]> getMonthlyRevenue() {
        return orderRepository.findMonthlyRevenue();
    }

    public List<Object[]> getTopSellingProducts() {
        return orderRepository.findTopSellingProducts(org.springframework.data.domain.PageRequest.of(0, 5));
    }

    public List<Object[]> getTopSellingProductsCurrentMonth() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return orderRepository.findTopSellingProductByMonth(now.getMonthValue(), now.getYear(),
                org.springframework.data.domain.PageRequest.of(0, 5));
    }

    public List<Order> searchOrders(String keyword) {
        return orderRepository.searchOrders(keyword);
    }

    // Paginated methods
    public org.springframework.data.domain.Page<Order> getAllOrdersPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<Order> getOrdersByStatusPaginated(OrderStatus status,
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    public org.springframework.data.domain.Page<Order> getOrdersByStatusesPaginated(List<OrderStatus> statuses,
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findByStatusIn(statuses, pageable);
    }

    public org.springframework.data.domain.Page<Order> searchOrdersPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.searchOrdersPaginated(keyword, pageable);
    }

    public org.springframework.data.domain.Page<Order> searchOrdersAndStatusPaginated(String keyword,
            OrderStatus status,
            org.springframework.data.domain.Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            if (status != null) {
                return orderRepository.searchOrdersAndStatusPaginated(keyword, status, pageable);
            } else {
                return orderRepository.searchOrdersPaginated(keyword, pageable);
            }
        } else {
            if (status != null) {
                return orderRepository.findByStatus(status, pageable);
            } else {
                return orderRepository.findAll(pageable);
            }
        }
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }
}
