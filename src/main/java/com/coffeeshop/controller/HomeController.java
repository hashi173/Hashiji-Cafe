package com.coffeeshop.controller;

import com.coffeeshop.entity.Product;
import com.coffeeshop.repository.JobPostingRepository;
import com.coffeeshop.service.CategoryService;
import com.coffeeshop.service.ProductService;
import com.coffeeshop.service.ToppingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ToppingService toppingService;
    private final JobPostingRepository jobPostingRepository;

    @GetMapping("/")
    public String home(Model model, jakarta.servlet.http.HttpServletRequest request) {
        // Force session creation for CSRF token
        request.getSession(true);
        // Products & categories for menu section
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        // Jobs for careers section (SPA)
        model.addAttribute("jobs", jobPostingRepository.findByIsActiveTrueOrderByCreatedAtDesc());
        return "home";
    }

    @GetMapping("/products/fragment")
    public String getProductsFragment(
            @org.springframework.web.bind.annotation.RequestParam(name = "categoryId", required = false) Long categoryId,
            Model model) {
        if (categoryId != null && categoryId == 0)
            categoryId = null; // Treat 0 as All
        model.addAttribute("products", productService.getProductsByCategory(categoryId));
        return "home :: productList"; // Return only the fragment
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));

        model.addAttribute("product", product);
        model.addAttribute("toppings", toppingService.getAllToppings());
        return "product/detail";
    }
}
