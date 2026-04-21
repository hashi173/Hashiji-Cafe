package com.coffeeshop.repository;

import com.coffeeshop.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, java.util.UUID> {

    @Query("SELECT SUM(e.amount) FROM Expense e")
    Double sumTotalExpenses();

    @Query("SELECT YEAR(e.expenseDate) as year, MONTH(e.expenseDate) as month, SUM(e.amount) as total FROM Expense e GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) ORDER BY year DESC, month DESC")
    java.util.List<Object[]> findMonthlyExpenses();

    java.util.List<Expense> findByExpenseDateBetween(java.time.LocalDate start, java.time.LocalDate end);

    @Query("SELECT e FROM Expense e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Expense> searchExpenses(@Param("keyword") String keyword);

    @Query("SELECT e FROM Expense e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Expense> searchExpensesPaginated(@Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = "SELECT e FROM Expense e", countQuery = "SELECT count(e) FROM Expense e")
    Page<Expense> findAllPaginated(Pageable pageable);
}
