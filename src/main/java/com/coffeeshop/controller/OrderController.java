package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderStatus;
import com.coffeeshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing customer orders.
 * Provides listing (with search/filter), detail view, status updates, and cancellation.
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private static final int PAGE_SIZE = 10;

    /** Lists orders in dual-table mode (Active + History) or single search-results mode. */
    @GetMapping
    public String listOrders(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "activePage", defaultValue = "0") int activePage,
            @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
            Model model) {

        PageRequest activeRequest = PageRequest.of(activePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest historyRequest = PageRequest.of(historyPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt"));

        List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);
        List<OrderStatus> historyStatuses = List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);

        if ((search != null && !search.isEmpty()) || (status != null)) {
            // Search/filter mode: single results table
            model.addAttribute("searchResults",
                    orderService.searchOrdersAndStatusPaginated(search, status, activeRequest));
            model.addAttribute("isSearching", true);
        } else {
            // Normal mode: dual tables (Active + History)
            Page<Order> activeOrdersPage = orderService.getOrdersByStatusesPaginated(activeStatuses, activeRequest);
            Page<Order> historyOrdersPage = orderService.getOrdersByStatusesPaginated(historyStatuses, historyRequest);

            model.addAttribute("activeOrders", activeOrdersPage.getContent());
            model.addAttribute("activePage", activePage);
            model.addAttribute("totalActivePages", activeOrdersPage.getTotalPages());

            model.addAttribute("historyOrders", historyOrdersPage.getContent());
            model.addAttribute("historyPage", historyPage);
            model.addAttribute("totalHistoryPages", historyOrdersPage.getTotalPages());
            model.addAttribute("isSearching", false);
        }

        model.addAttribute("search", search);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("totalItems", orderService.getTotalOrders());
        return "admin/orders/index";
    }

    /** Shows detailed view of a single order with items and status controls. */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable("id") Long id, Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders/detail";
    }

    /** Updates order status via dropdown (auto-submit). */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") OrderStatus status) {
        orderService.updateOrderStatus(id, status);
        return "redirect:/admin/orders/" + id;
    }

    /** Cancels an order (sets status to CANCELLED). */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id) {
        orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        return "redirect:/admin/orders/" + id;
    }
}
