package com.coffeeshop.controller;

import com.coffeeshop.entity.Category;
import com.coffeeshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 10;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                pageSize);
        org.springframework.data.domain.Page<Category> categoryPage;

        if (search != null && !search.isEmpty()) {
            categoryPage = categoryService.searchCategoriesPaginated(search, pageable);
        } else {
            categoryPage = categoryService.getAllCategoriesPaginated(pageable);
        }

        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("totalItems", categoryPage.getTotalElements());
        model.addAttribute("search", search);

        return "admin/categories/index";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@org.springframework.lang.NonNull @ModelAttribute Category category,
            RedirectAttributes ra) {
        categoryService.saveCategory(category);
        ra.addFlashAttribute("message", "Category saved successfully!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String editCategory(@org.springframework.lang.NonNull @PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        model.addAttribute("category", category);
        return "admin/categories/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            categoryService.deleteCategory(id);
            ra.addFlashAttribute("message", "Category deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete category. It might contain products.");
        }
        return "redirect:/admin/categories";
    }
}
