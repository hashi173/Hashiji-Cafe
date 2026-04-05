package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkShift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "start_cash")
    private Double startCash;

    @Column(name = "end_cash")
    private Double endCash;

    @Column(name = "total_revenue")
    private Double totalRevenue;

    @Column(name = "cash_variance")
    private Double cashVariance; // Actual - Expected

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShiftStatus status;

    @Transient
    public Double getEstimatedPay() {
        if (startTime == null || user == null || user.getHourlyRate() == null) {
            return 0.0;
        }
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        long minutes = java.time.Duration.between(startTime, end).toMinutes();
        return (minutes / 60.0) * user.getHourlyRate();
    }
}
