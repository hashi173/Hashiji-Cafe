package com.coffeeshop.service;

import com.coffeeshop.entity.User;
import com.coffeeshop.entity.WorkShift;
import com.coffeeshop.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkShiftService {

    private final WorkShiftRepository workShiftRepository;
    private final com.coffeeshop.repository.OrderRepository orderRepository;

    public WorkShift startShift(User user, Double startCash) {
        // Check if already open
        Optional<WorkShift> existing = workShiftRepository.findByUserAndStatus(user,
                com.coffeeshop.entity.ShiftStatus.OPEN);
        if (existing.isPresent()) {
            return existing.get();
        }

        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setStartTime(LocalDateTime.now());
        shift.setStartCash(startCash);
        shift.setStatus(com.coffeeshop.entity.ShiftStatus.OPEN);
        shift.setTotalRevenue(0.0);
        return workShiftRepository.save(shift);
    }

    public void endShift(User user, Double endCash) {
        WorkShift shift = workShiftRepository.findByUserAndStatus(user, com.coffeeshop.entity.ShiftStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No open shift found"));

        shift.setEndTime(LocalDateTime.now());
        shift.setEndCash(endCash);
        shift.setStatus(com.coffeeshop.entity.ShiftStatus.CLOSED);

        // Calculate total revenue from Orders
        Double revenue = orderRepository.sumRevenueByUserAndDateRange(
                user,
                shift.getStartTime(),
                shift.getEndTime());

        double actualRevenue = revenue != null ? revenue : 0.0;
        shift.setTotalRevenue(actualRevenue);

        // Expected Cash = Start Cash + Revenue (Assuming all cash for MVP)
        // In real app, subtract card payments
        double expected = shift.getStartCash() + actualRevenue;
        shift.setCashVariance(endCash - expected);

        workShiftRepository.save(shift);
    }

    public Optional<WorkShift> getCurrentShift(User user) {
        return workShiftRepository.findByUserAndStatus(user, com.coffeeshop.entity.ShiftStatus.OPEN);
    }

    public org.springframework.data.domain.Page<WorkShift> getAllShiftsPaginated(
            org.springframework.data.domain.Pageable pageable) {
        return workShiftRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<WorkShift> searchShiftsPaginated(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        return workShiftRepository.searchShiftsPaginated(keyword, pageable);
    }

    public Double calculateTotalPayroll() {
        // This is an estimation based on CLOSED shifts
        // Ideally done via DB Query: SUM(TIMESTAMPDIFF(HOUR, start, end) *
        // user.hourlyRate)
        // But for simplicity and to handle complex hourly rates, we can loop in Java
        // for now (MVP)

        // Better implementation:
        return workShiftRepository.findAll().stream()
                .filter(s -> com.coffeeshop.entity.ShiftStatus.CLOSED.equals(s.getStatus()) && s.getEndTime() != null)
                .mapToDouble(s -> {
                    double hours = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes() / 60.0;
                    Double rate = s.getUser().getHourlyRate();
                    return hours * (rate != null ? rate : 0.0);
                })
                .sum();
    }
}
