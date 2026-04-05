package com.coffeeshop.repository;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// Order Repository

public interface OrderRepository extends JpaRepository<Order, Long> {
        List<Order> findByUserId(Long userId);

        long countByStatus(OrderStatus status);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
        Double sumTotalAmountByStatus(@Param("status") OrderStatus status);

        java.util.Optional<Order> findByTrackingCode(String trackingCode);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user = :user AND o.createdAt BETWEEN :start AND :end AND o.status = com.coffeeshop.entity.OrderStatus.COMPLETED")
        Double sumRevenueByUserAndDateRange(@Param("user") com.coffeeshop.entity.User user,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT FUNCTION('DATE', o.createdAt) as date, SUM(o.totalAmount) as total FROM Order o WHERE o.status = com.coffeeshop.entity.OrderStatus.COMPLETED GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY date ASC")
        List<Object[]> findDailyRevenue();

        @Query("SELECT d.productName, SUM(d.quantity) FROM OrderDetail d JOIN d.order o WHERE o.status = com.coffeeshop.entity.OrderStatus.COMPLETED GROUP BY d.productName ORDER BY SUM(d.quantity) DESC")
        List<Object[]> findTopSellingProducts(org.springframework.data.domain.Pageable pageable);

        @Query("SELECT YEAR(o.createdAt) as year, MONTH(o.createdAt) as month, SUM(o.totalAmount) as total FROM Order o WHERE o.status = com.coffeeshop.entity.OrderStatus.COMPLETED GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY year DESC, month DESC")
        List<Object[]> findMonthlyRevenue();

        @Query("SELECT d.productName, SUM(d.quantity), MAX(p.image) " +
                        "FROM OrderDetail d " +
                        "JOIN d.order o " +
                        "LEFT JOIN d.product p " +
                        "WHERE o.status = com.coffeeshop.entity.OrderStatus.COMPLETED AND MONTH(o.createdAt) = :month AND YEAR(o.createdAt) = :year "
                        +
                        "GROUP BY d.productName " +
                        "ORDER BY SUM(d.quantity) DESC")
        List<Object[]> findTopSellingProductByMonth(@Param("month") int month, @Param("year") int year,
                        org.springframework.data.domain.Pageable pageable);

        List<Order> findAllByStatusAndCreatedAtBetween(OrderStatus status, java.time.LocalDateTime start,
                        java.time.LocalDateTime end);

        org.springframework.data.domain.Page<Order> findAllByStatusAndCreatedAtBetween(OrderStatus status,
                        java.time.LocalDateTime start,
                        java.time.LocalDateTime end, org.springframework.data.domain.Pageable pageable);

        List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

        List<Order> findAllByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);

        @Query("SELECT o FROM Order o WHERE " +
                        "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "o.phone LIKE CONCAT('%', :keyword, '%') OR " +
                        "CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')")
        List<Order> searchOrders(@Param("keyword") String keyword);

        // Paginated methods
        Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

        Page<Order> findByStatus(OrderStatus status, Pageable pageable);

        Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

        @Query("SELECT o FROM Order o WHERE " +
                        "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "o.phone LIKE CONCAT('%', :keyword, '%') OR " +
                        "CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')")
        Page<Order> searchOrdersPaginated(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT o FROM Order o WHERE " +
                        "(LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "o.phone LIKE CONCAT('%', :keyword, '%') OR " +
                        "CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
                        "o.status = :status")
        Page<Order> searchOrdersAndStatusPaginated(@Param("keyword") String keyword,
                        @Param("status") OrderStatus status, Pageable pageable);
}
