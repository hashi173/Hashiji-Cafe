package com.coffeeshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

@Controller
public class PageController {

    @Autowired
    private com.coffeeshop.repository.JobApplicationRepository jobApplicationRepository;

    @Autowired
    private com.coffeeshop.repository.JobPostingRepository jobPostingRepository;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/about")
    public String about() {
        return "redirect:/#about";
    }

    @GetMapping("/active")
    public String active() {
        return "redirect:/";
    }

    @GetMapping("/careers")
    public String careers() {
        return "redirect:/#careers";
    }

    @org.springframework.web.bind.annotation.PostMapping("/careers/apply")
    public String applyForJob(@org.springframework.web.bind.annotation.RequestParam("fullName") String fullName,
            @org.springframework.web.bind.annotation.RequestParam("email") String email,
            @org.springframework.web.bind.annotation.RequestParam("phone") String phone,
            @org.springframework.web.bind.annotation.RequestParam("position") String position,
            @org.springframework.web.bind.annotation.RequestParam("cvFile") org.springframework.web.multipart.MultipartFile cvFile,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            java.util.Locale locale) {

        try {
            com.coffeeshop.entity.JobApplication application = new com.coffeeshop.entity.JobApplication();
            application.setFullName(fullName);
            application.setEmail(email);
            application.setPhone(phone);
            application.setPosition(position);

            if (!cvFile.isEmpty()) {
                String fileName = java.util.UUID.randomUUID().toString() + "_" + cvFile.getOriginalFilename();
                java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/cv");
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                java.nio.file.Files.copy(cvFile.getInputStream(), uploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                application.setCvUrl("/uploads/cv/" + fileName);
            }

            // Generate Tracking Code
            String trackingCode = "CV-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            application.setTrackingCode(trackingCode);

            jobApplicationRepository.save(application);
            String successMsg = messageSource.getMessage("careers.apply.success", null, locale);
            redirectAttributes.addFlashAttribute("success", successMsg);
            redirectAttributes.addFlashAttribute("trackingCode", trackingCode);

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = messageSource.getMessage("careers.apply.error", null, locale);
            redirectAttributes.addFlashAttribute("error", errorMsg);
        }

        return "redirect:/#careers";
    }

    @GetMapping("/info")
    public String info() {
        return "redirect:/#info";
    }

    @org.springframework.web.bind.annotation.PostMapping("/contact")
    public String contact(org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            java.util.Locale locale) {
        // In a real app, save message to DB or send email
        String successMsg = messageSource.getMessage("contact.success", null, locale);
        redirectAttributes.addFlashAttribute("success", successMsg);
        return "redirect:/#info";
    }
}
