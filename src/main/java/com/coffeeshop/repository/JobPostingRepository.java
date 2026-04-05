package com.coffeeshop.repository;

import com.coffeeshop.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@org.springframework.stereotype.Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
        List<JobPosting> findByIsActiveTrueOrderByCreatedAtDesc();

        @org.springframework.data.jpa.repository.Query("SELECT j FROM JobPosting j WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<JobPosting> searchJobs(@org.springframework.data.repository.query.Param("keyword") String keyword);

        @org.springframework.data.jpa.repository.Query("SELECT j FROM JobPosting j WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        org.springframework.data.domain.Page<JobPosting> searchJobsPaginated(
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<JobPosting> findAll(org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<JobPosting> findByIsActive(boolean isActive,
                        org.springframework.data.domain.Pageable pageable);
}
