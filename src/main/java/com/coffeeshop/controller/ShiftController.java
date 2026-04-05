package com.coffeeshop.controller;

import com.coffeeshop.entity.WorkShift;
import com.coffeeshop.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/shifts")
@RequiredArgsConstructor
public class ShiftController {

        private final WorkShiftRepository workShiftRepository;
        private final com.coffeeshop.repository.OrderRepository orderRepository;
        private final com.coffeeshop.repository.UserRepository userRepository;

        @GetMapping
        public String listShifts(
                        @org.springframework.web.bind.annotation.RequestParam(value = "search", required = false) String search,
                        @org.springframework.web.bind.annotation.RequestParam(value = "activePage", defaultValue = "0") int activePage,
                        @org.springframework.web.bind.annotation.RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                        Model model) {

                int pageSize = 10;

                if (search != null && !search.isEmpty()) {
                        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest
                                        .of(activePage, pageSize, org.springframework.data.domain.Sort.by("startTime")
                                                        .descending());
                        org.springframework.data.domain.Page<WorkShift> shiftPage = workShiftRepository
                                        .searchShiftsPaginated(search, pageable);

                        model.addAttribute("searchResults", shiftPage.getContent());
                        model.addAttribute("shiftPage", shiftPage); // compatibility
                        model.addAttribute("currentPage", activePage);
                        model.addAttribute("totalPages", shiftPage.getTotalPages());
                        model.addAttribute("totalItems", shiftPage.getTotalElements());
                        model.addAttribute("isSearching", true);
                } else {
                        org.springframework.data.domain.Pageable activeRequest = org.springframework.data.domain.PageRequest
                                        .of(activePage, pageSize, org.springframework.data.domain.Sort.by("startTime")
                                                        .descending());
                        org.springframework.data.domain.Pageable historyRequest = org.springframework.data.domain.PageRequest
                                        .of(historyPage, pageSize, org.springframework.data.domain.Sort.by("startTime")
                                                        .descending());

                        org.springframework.data.domain.Page<WorkShift> activeShiftPage = workShiftRepository
                                        .findByStatus(com.coffeeshop.entity.ShiftStatus.OPEN, activeRequest);
                        org.springframework.data.domain.Page<WorkShift> historyShiftPage = workShiftRepository
                                        .findByStatus(com.coffeeshop.entity.ShiftStatus.CLOSED, historyRequest);

                        model.addAttribute("activeShifts", activeShiftPage.getContent());
                        model.addAttribute("activePage", activePage);
                        model.addAttribute("totalActivePages", activeShiftPage.getTotalPages());
                        model.addAttribute("totalActiveCount", activeShiftPage.getTotalElements());

                        model.addAttribute("historyShifts", historyShiftPage.getContent());
                        model.addAttribute("historyPage", historyPage);
                        model.addAttribute("totalHistoryPages", historyShiftPage.getTotalPages());
                        model.addAttribute("totalHistoryCount", historyShiftPage.getTotalElements());

                        model.addAttribute("isSearching", false);
                }

                model.addAttribute("users", userRepository.findAll());
                model.addAttribute("search", search);
                return "admin/shifts/index";
        }

        @GetMapping("/{id}")
        public String viewShift(@org.springframework.web.bind.annotation.PathVariable("id") Long id, Model model) {
                WorkShift shift = workShiftRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Shift not found"));

                // Find orders associated with this shift
                // Logic: Orders by this User, between StartTime and EndTime (or Now if open)
                java.time.LocalDateTime end = shift.getEndTime() != null ? shift.getEndTime()
                                : java.time.LocalDateTime.now();

                List<com.coffeeshop.entity.Order> orders = orderRepository.findAll().stream()
                                .filter(o -> o.getUser() != null && o.getUser().getId().equals(shift.getUser().getId()))
                                .filter(o -> o.getCreatedAt().isAfter(shift.getStartTime())
                                                && o.getCreatedAt().isBefore(end))
                                .collect(java.util.stream.Collectors.toList());

                // Calculate Product Summary
                java.util.Map<String, Integer> productSummary = new java.util.HashMap<>();
                for (com.coffeeshop.entity.Order order : orders) {
                        for (com.coffeeshop.entity.OrderDetail detail : order.getOrderDetails()) {
                                String key = detail.getProductName() + " (" + detail.getSizeSelected() + ")";
                                productSummary.put(key, productSummary.getOrDefault(key, 0) + detail.getQuantity());
                        }
                }

                model.addAttribute("shift", shift);
                model.addAttribute("orders", orders);
                model.addAttribute("productSummary", productSummary);
                return "admin/shifts/view";
        }

        @org.springframework.web.bind.annotation.PostMapping("/create")
        public String createShift(
                        @org.springframework.web.bind.annotation.RequestParam Long userId,
                        @org.springframework.web.bind.annotation.RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
                        @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime) {

                com.coffeeshop.entity.User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (endTime != null && startTime.isAfter(endTime)) {
                        // Basic validation
                        return "redirect:/admin/shifts?error=InvalidTime";
                }

                WorkShift shift = new WorkShift();
                shift.setUser(user);
                shift.setStartTime(startTime);
                shift.setStartCash(0.0);

                if (endTime != null) {
                        shift.setEndTime(endTime);
                        shift.setStatus(com.coffeeshop.entity.ShiftStatus.CLOSED);
                        shift.setTotalRevenue(0.0); // Manual entry assumes 0 pos revenue unless updated
                } else {
                        // Check if user already has open shift?
                        // WorkShiftService checks this, but here we override for admin manual entry?
                        // Better to check to avoid duplicates.
                        if (workShiftRepository.findByUserAndStatus(user, com.coffeeshop.entity.ShiftStatus.OPEN)
                                        .isPresent()) {
                                return "redirect:/admin/shifts?error=UserHasOpenShift";
                        }
                        shift.setStatus(com.coffeeshop.entity.ShiftStatus.OPEN);
                        shift.setTotalRevenue(0.0);
                }

                workShiftRepository.save(shift);
                return "redirect:/admin/shifts";
        }

        @org.springframework.web.bind.annotation.PostMapping("/{id}/delete")
        public String deleteShift(@org.springframework.web.bind.annotation.PathVariable Long id) {
                workShiftRepository.deleteById(id);
                return "redirect:/admin/shifts";
        }
}
