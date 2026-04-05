package com.coffeeshop.controller;

import com.coffeeshop.entity.Ingredient;
import com.coffeeshop.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String listIngredients(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("name").ascending());
        Page<Ingredient> ingredientPage = ingredientRepository.findAll(pageable);

        model.addAttribute("ingredients", ingredientPage.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", ingredientPage.getTotalPages());
        model.addAttribute("totalElements", ingredientPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("newIngredient", new Ingredient());
        return "admin/ingredients/index";
    }

    @PostMapping("/save")
    public String saveIngredient(@ModelAttribute Ingredient ingredient, RedirectAttributes ra) {
        ingredientRepository.save(ingredient);
        ra.addFlashAttribute("message", "Ingredient saved successfully!");
        return "redirect:/admin/ingredients";
    }

    @GetMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable Long id, RedirectAttributes ra) {
        ingredientRepository.deleteById(id);
        ra.addFlashAttribute("message", "Ingredient deleted!");
        return "redirect:/admin/ingredients";
    }

    @PostMapping("/restock/{id}")
    public String restockIngredient(@PathVariable Long id, @RequestParam Double amount, RedirectAttributes ra) {
        ingredientRepository.findById(id).ifPresent(ingredient -> {
            ingredient.setStockQuantity(ingredient.getStockQuantity() + amount);
            ingredientRepository.save(ingredient);
        });
        ra.addFlashAttribute("message", "Stock updated!");
        return "redirect:/admin/ingredients";
    }
}
