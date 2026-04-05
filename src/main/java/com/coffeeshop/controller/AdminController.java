package com.coffeeshop.controller;

import com.coffeeshop.repository.ExpenseRepository;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.WorkShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin dashboard controller.
 * Aggregates KPIs (revenue, orders, expenses, payroll) and chart data
 * for the main admin overview page.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;
    private final ExpenseRepository expenseRepository;
    private final WorkShiftService workShiftService;

    /** Renders the admin dashboard with KPI cards and Chart.js data. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // --- KPI Metrics ---
        long totalOrders = orderService.countTotalOrders();
        long pendingOrders = orderService.countPendingOrders();
        double totalRevenue = orderService.calculateTotalRevenue();

        Double expenses = expenseRepository.sumTotalExpenses();
        double totalExpenses = expenses != null ? expenses : 0.0;
        double totalPayroll = workShiftService.calculateTotalPayroll();
        double netProfit = totalRevenue - (totalExpenses + totalPayroll);

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalPayroll", totalPayroll);
        model.addAttribute("netProfit", netProfit);

        // --- Revenue Chart Data (reversed to chronological ASC) ---
        List<Object[]> revenueData = orderService.getMonthlyRevenue();
        List<String> revenueDates = new ArrayList<>();
        List<Double> revenueAmounts = new ArrayList<>();

        for (int i = revenueData.size() - 1; i >= 0; i--) {
            Object[] row = revenueData.get(i);
            revenueDates.add(row[1] + "/" + row[0]);
            revenueAmounts.add(row[2] != null ? Double.valueOf(row[2].toString()) : 0.0);
        }

        // Fallback: show current month with zero revenue to avoid empty chart
        if (revenueDates.isEmpty()) {
            LocalDate now = LocalDate.now();
            revenueDates.add(now.getMonthValue() + "/" + now.getYear());
            revenueAmounts.add(0.0);
        }

        // --- Top Products This Month ---
        List<Object[]> topProducts = orderService.getTopSellingProductsCurrentMonth();
        List<String> productNames = new ArrayList<>();
        List<Long> productQuantities = new ArrayList<>();
        List<String> productImages = new ArrayList<>();

        for (Object[] row : topProducts) {
            productNames.add((String) row[0]);
            productQuantities.add(((Number) row[1]).longValue());

            String img = row[2] != null ? (String) row[2] : null;
            if (img == null || img.isEmpty()) {
                img = "/images/no-image.png";
            } else if (!img.startsWith("http") && !img.startsWith("/")) {
                img = "/images/products/" + img;
            }
            productImages.add(img);
        }

        model.addAttribute("revenueDates", revenueDates);
        model.addAttribute("revenueAmounts", revenueAmounts);
        model.addAttribute("productNames", productNames);
        model.addAttribute("productQuantities", productQuantities);
        model.addAttribute("productImages", productImages);

        return "admin/dashboard";
    }
}
