package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderStatus;
import com.coffeeshop.entity.Expense;
import com.coffeeshop.repository.ExpenseRepository;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for monthly financial history reporting.
 * Uses database-level aggregation queries for performance.
 */
@Controller
@RequestMapping("/admin/history")
@RequiredArgsConstructor
public class AdminHistoryController {

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;

    /** Displays the monthly financial history overview with chart data. */
    @GetMapping
    public String history(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Map<String, MonthlyStats> statsMap = new HashMap<>();

        try {
            // 1. Efficient Monthly Revenue Aggregation
            List<Object[]> revenueData = orderRepository.findMonthlyRevenue();
            for (Object[] row : revenueData) {
                int year = ((Number) row[0]).intValue();
                int month = ((Number) row[1]).intValue();
                double total = ((Number) row[2]).doubleValue();
                String key = year + "-" + month;
                
                statsMap.putIfAbsent(key, new MonthlyStats(year, month));
                statsMap.get(key).setRevenue(total);
            }

            // 2. Efficient Monthly Expense Aggregation
            List<Object[]> expenseData = expenseRepository.findMonthlyExpenses();
            for (Object[] row : expenseData) {
                int year = ((Number) row[0]).intValue();
                int month = ((Number) row[1]).intValue();
                double total = ((Number) row[2]).doubleValue();
                String key = year + "-" + month;

                statsMap.putIfAbsent(key, new MonthlyStats(year, month));
                statsMap.get(key).setExpenses(total);
            }

            // 3. (Optional) For Top Product, we usually handle it in the details view for performance, 
            // but if needed here, we would need a more complex query. 
            // For now, let's keep it simple to fix the hanging.
            for (MonthlyStats stat : statsMap.values()) {
                stat.setTopProductName("Check Details");
                stat.setTopProductImage("https://placehold.co/40");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }



        // Sort descending by year then month
        List<MonthlyStats> monthlyStats = new ArrayList<>(statsMap.values());
        monthlyStats.sort((a, b) -> {
            if (a.year != b.year) return b.year - a.year;
            return b.month - a.month;
        });

        // Ensure at least current month is shown
        if (monthlyStats.isEmpty()) {
            java.time.LocalDate now = java.time.LocalDate.now();
            monthlyStats.add(new MonthlyStats(now.getYear(), now.getMonthValue()));
        }

        // Search filter
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            monthlyStats = monthlyStats.stream()
                    .filter(s -> s.getMonthName().toLowerCase().contains(lowerSearch) ||
                            String.valueOf(s.year).contains(lowerSearch) ||
                            s.getTopProductName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        // Pagination Logic
        int pageSize = 10;
        int totalItems = monthlyStats.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // Clamp page
        if (page < 0)
            page = 0;
        if (page >= totalPages && totalPages > 0)
            page = totalPages - 1;

        int start = page * pageSize;
        int end = Math.min(start + pageSize, totalItems);

        List<MonthlyStats> pagedStats = (start > totalItems) ? new ArrayList<>() : monthlyStats.subList(start, end);

        model.addAttribute("monthlyStats", pagedStats);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("search", search);

        // Prepare Chart Data (Use FULL LIST, specifically last 12 months)
        List<String> labels = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();
        List<Double> profits = new ArrayList<>();

        // Prepare chart data (last 12 months, reversed for chronological order)
        int chartLimit = Math.min(monthlyStats.size(), 12);
        for (int i = chartLimit - 1; i >= 0; i--) {
            MonthlyStats stat = monthlyStats.get(i);
            labels.add(Month.of(stat.month).name().substring(0, 3) + " " + stat.year);
            revenues.add(stat.revenue);
            expenses.add(stat.expenses);
            profits.add(stat.revenue - stat.expenses);
        }

        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartRevenues", revenues);
        model.addAttribute("chartExpenses", expenses);
        model.addAttribute("chartProfits", profits);

        return "admin/history";
    }

    /** Shows detailed breakdown of orders and expenses for a specific month. */
    @GetMapping("/details")
    public String details(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Orders Pagination
        int pageSize = 20;
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Order> ordersPage = orderRepository.findAllByStatusAndCreatedAtBetween(
                OrderStatus.COMPLETED, start, end, pageable);

        List<Expense> expenseList = expenseRepository.findByExpenseDateBetween(
                yearMonth.atDay(1), yearMonth.atEndOfMonth());

        // Revenue needs full-month total, not just current page
        List<Order> allOrdersForSum = orderRepository.findAllByStatusAndCreatedAtBetween(
                OrderStatus.COMPLETED, start, end);
        double revenueSum = allOrdersForSum.stream().mapToDouble(Order::getTotalAmount).sum();
        double totalExpenses = expenseList.stream().mapToDouble(Expense::getAmount).sum();

        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("monthName", Month.of(month).name());

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());

        model.addAttribute("expenses", expenseList);
        model.addAttribute("totalRevenue", revenueSum);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("netProfit", revenueSum - totalExpenses);

        return "admin/history_details";
    }

    /** Inner DTO for monthly aggregated statistics. */
    @lombok.Data
    public static class MonthlyStats {
        private int year;
        private int month;
        private double revenue = 0.0;
        private double expenses = 0.0;
        private String topProductName = "N/A";
        private long topProductQuantity = 0;
        private String topProductImage;

        public MonthlyStats(int year, int month) {
            this.year = year;
            this.month = month;
        }

        public double getNetProfit() {
            return revenue - expenses;
        }

        public String getMonthName() {
            return Month.of(month).name();
        }
    }
}
