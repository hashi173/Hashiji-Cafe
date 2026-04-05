package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    // Optional link to specific posting
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    // Snapshot of position in case JobPosting is deleted or generic application
    @Column(nullable = false, length = 50)
    private String position;

    @Column(name = "tracking_code", unique = true, length = 50)
    private String trackingCode;

    @Column(length = 500)
    private String cvUrl; // Path to the uploaded CV file

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.NEW;

    public enum ApplicationStatus {
        NEW, REVIEWED, INTERVIEWING, REJECTED, HIRED
    }
}
