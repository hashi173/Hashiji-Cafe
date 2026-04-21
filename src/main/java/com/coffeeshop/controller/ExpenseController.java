package com.coffeeshop.controller;

import com.coffeeshop.entity.Expense;
import com.coffeeshop.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String listExpenses(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 20;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                pageSize, org.springframework.data.domain.Sort.by("expenseDate").descending());
        org.springframework.data.domain.Page<Expense> expensePage;

        if (search != null && !search.isEmpty()) {
            expensePage = expenseService.searchExpensesPaginated(search, pageable);
        } else {
            expensePage = expenseService.getAllExpensesPaginated(pageable);
        }

        model.addAttribute("expenses", expensePage.getContent());
        model.addAttribute("expensePage", expensePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expensePage.getTotalPages());
        model.addAttribute("totalItems", expensePage.getTotalElements());
        model.addAttribute("search", search);

        return "admin/expenses/index";
    }

    @PostMapping("/save")
    public String saveExpense(@org.springframework.lang.NonNull @ModelAttribute Expense expense) {
        expenseService.saveExpense(expense);
        return "redirect:/admin/expenses";
    }

    @GetMapping("/delete/{id}")
    public String deleteExpense(@org.springframework.lang.NonNull @PathVariable java.util.UUID id) {
        expenseService.deleteExpense(id);
        return "redirect:/admin/expenses";
    }
}
