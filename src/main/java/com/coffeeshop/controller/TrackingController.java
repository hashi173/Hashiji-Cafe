package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final OrderRepository orderRepository;
    private final com.coffeeshop.repository.JobApplicationRepository jobApplicationRepository;
    private final org.springframework.context.MessageSource messageSource;

    @GetMapping
    public String showTrackingPage() {
        return "tracking/index";
    }

    @GetMapping("/search")
    public String trackOrder(@RequestParam("code") String code, Model model, java.util.Locale locale) {
        Order order = orderRepository.findByTrackingCode(code.trim()).orElse(null);

        if (order != null) {
            model.addAttribute("order", order);
        } else {
            // Try searching for job application
            com.coffeeshop.entity.JobApplication application = jobApplicationRepository.findByTrackingCode(code.trim())
                    .orElse(null);
            if (application != null) {
                model.addAttribute("jobApplication", application);
            } else {
                String errorMsg = messageSource.getMessage("error.order_not_found", new Object[] { code }, locale);
                model.addAttribute("error", errorMsg);
            }
        }
        return "tracking/index";
    }

    @org.springframework.web.bind.annotation.PostMapping("/cancel")
    public String cancelOrder(@RequestParam("orderId") Long orderId,
            @RequestParam("trackingCode") String trackingCode,
            @RequestParam(value = "reason", required = false) String reason,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (orderId == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid order ID.");
            return "redirect:/tracking/search?code=" + (trackingCode != null ? trackingCode : "");
        }
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order != null && trackingCode.equals(order.getTrackingCode())) {
            if (order.getStatus() == com.coffeeshop.entity.OrderStatus.PENDING) {
                order.setStatus(com.coffeeshop.entity.OrderStatus.CANCELLED);
                // In a real app, save the reason log
                orderRepository.save(order);
                redirectAttributes.addFlashAttribute("success", "Order cancelled successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Cannot cancel order. It is already " + order.getStatus());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid order details.");
        }

        return "redirect:/tracking/search?code=" + trackingCode;
    }
}
