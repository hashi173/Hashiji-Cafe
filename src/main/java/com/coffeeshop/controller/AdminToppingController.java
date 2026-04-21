package com.coffeeshop.controller;

import com.coffeeshop.entity.Topping;
import com.coffeeshop.repository.ToppingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/toppings")
@RequiredArgsConstructor
public class AdminToppingController {

    private final ToppingRepository toppingRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("toppings", toppingRepository.findAll());
        model.addAttribute("newTopping", new Topping());
        return "admin/toppings/index";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Topping topping, RedirectAttributes ra) {
        try {
            toppingRepository.save(topping);
            ra.addFlashAttribute("success", "Topping saved successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error saving topping: " + e.getMessage());
        }
        return "redirect:/admin/toppings";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable java.util.UUID id, RedirectAttributes ra) {
        try {
            toppingRepository.deleteById(id);
            ra.addFlashAttribute("success", "Topping deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cannot delete topping. It might be in use.");
        }
        return "redirect:/admin/toppings";
    }
}
