package com.coffeeshop.repository;

import com.coffeeshop.entity.User;
import com.coffeeshop.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    Optional<WorkShift> findByUserAndStatus(User user, com.coffeeshop.entity.ShiftStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WorkShift w WHERE LOWER(w.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY w.startTime DESC")
    java.util.List<WorkShift> searchShifts(@org.springframework.data.repository.query.Param("keyword") String keyword);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WorkShift w WHERE LOWER(w.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY w.startTime DESC")
    org.springframework.data.domain.Page<WorkShift> searchShiftsPaginated(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<WorkShift> findAll(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<WorkShift> findByStatus(com.coffeeshop.entity.ShiftStatus status,
            org.springframework.data.domain.Pageable pageable);
}
