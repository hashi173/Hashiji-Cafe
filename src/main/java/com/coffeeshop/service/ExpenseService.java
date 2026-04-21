package com.coffeeshop.service;

import com.coffeeshop.entity.Expense;
import com.coffeeshop.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense saveExpense(@org.springframework.lang.NonNull Expense expense) {
        return expenseRepository.save(expense);
    }

    public void deleteExpense(@org.springframework.lang.NonNull java.util.UUID id) {
        expenseRepository.deleteById(id);
    }

    public List<Expense> searchExpenses(String keyword) {
        return expenseRepository.searchExpenses(keyword);
    }

    public org.springframework.data.domain.Page<Expense> getAllExpensesPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return expenseRepository.findAllPaginated(pageable);
    }

    public org.springframework.data.domain.Page<Expense> searchExpensesPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return expenseRepository.searchExpensesPaginated(keyword, pageable);
    }

}
