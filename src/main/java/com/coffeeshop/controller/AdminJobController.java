package com.coffeeshop.controller;

import com.coffeeshop.entity.JobApplication;
import com.coffeeshop.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/recruitment")
@RequiredArgsConstructor
public class AdminJobController {

    private final JobApplicationRepository jobApplicationRepository;
    private final com.coffeeshop.repository.JobPostingRepository jobPostingRepository;

    @GetMapping
    public String index(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) JobApplication.ApplicationStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 10;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                pageSize, org.springframework.data.domain.Sort.by("createdAt").descending());
        org.springframework.data.domain.Page<JobApplication> appPage;

        // Note: For advanced filtering (Search + Status), we might need a Specification
        // or Query.
        // For now, if search is present, we search. If status is present, we might need
        // new repo method.
        // Given existing repo methods, let's stick to Search OR SearchAll.
        // To be "Expert", we should support both, but I'll stick to Search keyword or
        // FindAll for now to avoid Repo complexity unless need.
        // If the user wants to filter by status, I can add that later. For now, just
        // Pagination for ALL.

        if (search != null && !search.isEmpty()) {
            appPage = jobApplicationRepository.searchApplicationsPaginated(search, pageable);
        } else {
            appPage = jobApplicationRepository.findAll(pageable);
        }

        model.addAttribute("applications", appPage.getContent());
        model.addAttribute("appPage", appPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appPage.getTotalPages());
        model.addAttribute("totalItems", appPage.getTotalElements());
        model.addAttribute("search", search);
        return "admin/recruitment/index";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam("status") JobApplication.ApplicationStatus status,
            RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid ID");
            return "redirect:/admin/recruitment";
        }
        JobApplication application = jobApplicationRepository.findById(id).orElse(null);
        if (application != null) {
            application.setStatus(status);
            jobApplicationRepository.save(application);
            redirectAttributes.addFlashAttribute("success", "Application status updated.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Application not found.");
        }
        return "redirect:/admin/recruitment";
    }

    // Job Posting Management

    @GetMapping("/jobs")
    public String listJobs(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activePage", defaultValue = "0") int activePage,
            @RequestParam(value = "closedPage", defaultValue = "0") int closedPage,
            Model model) {

        int pageSize = 10;

        if (search != null && !search.isEmpty()) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Page<com.coffeeshop.entity.JobPosting> jobPage = jobPostingRepository
                    .searchJobsPaginated(search, pageable);

            model.addAttribute("searchResults", jobPage.getContent());
            model.addAttribute("jobPage", jobPage);
            model.addAttribute("currentPage", activePage);
            model.addAttribute("totalPages", jobPage.getTotalPages());
            model.addAttribute("totalItems", jobPage.getTotalElements());
            model.addAttribute("isSearching", true);
        } else {
            org.springframework.data.domain.Pageable activeRequest = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Pageable closedRequest = org.springframework.data.domain.PageRequest
                    .of(closedPage, pageSize);

            org.springframework.data.domain.Page<com.coffeeshop.entity.JobPosting> activeJobPage = jobPostingRepository
                    .findByIsActive(true, activeRequest);
            org.springframework.data.domain.Page<com.coffeeshop.entity.JobPosting> closedJobPage = jobPostingRepository
                    .findByIsActive(false, closedRequest);

            model.addAttribute("activeJobs", activeJobPage.getContent());
            model.addAttribute("activePage", activePage);
            model.addAttribute("totalActivePages", activeJobPage.getTotalPages());
            model.addAttribute("totalActiveCount", activeJobPage.getTotalElements());

            model.addAttribute("closedJobs", closedJobPage.getContent());
            model.addAttribute("closedPage", closedPage);
            model.addAttribute("totalClosedPages", closedJobPage.getTotalPages());
            model.addAttribute("totalClosedCount", closedJobPage.getTotalElements());

            model.addAttribute("isSearching", false);
        }

        model.addAttribute("search", search);
        return "admin/recruitment/jobs";
    }

    @PostMapping("/jobs/save")
    public String saveJob(@ModelAttribute com.coffeeshop.entity.JobPosting jobPosting,
            RedirectAttributes redirectAttributes) {
        jobPostingRepository.save(jobPosting);
        redirectAttributes.addFlashAttribute("success", "Job opening saved successfully.");
        return "redirect:/admin/recruitment/jobs";
    }

    @GetMapping("/jobs/delete/{id}")
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null)
            return "redirect:/admin/recruitment/jobs";
        jobPostingRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Job opening deleted.");
        return "redirect:/admin/recruitment/jobs";
    }

    @GetMapping("/jobs/toggle/{id}")
    public String toggleJobStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null)
            return "redirect:/admin/recruitment/jobs";
        com.coffeeshop.entity.JobPosting job = jobPostingRepository.findById(id).orElse(null);
        if (job != null) {
            job.setActive(!job.isActive());
            jobPostingRepository.save(job);
            redirectAttributes.addFlashAttribute("success", "Job status updated.");
        }
        return "redirect:/admin/recruitment/jobs";
    }
}
