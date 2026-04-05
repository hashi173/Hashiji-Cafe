package com.coffeeshop.controller;

import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductSize;
import com.coffeeshop.entity.Ingredient;
import com.coffeeshop.service.CategoryService;
import com.coffeeshop.service.ProductService;
import com.coffeeshop.repository.IngredientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final IngredientRepository ingredientRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String listProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activePage", defaultValue = "0") int activePage,
            @RequestParam(value = "inactivePage", defaultValue = "0") int inactivePage,
            Model model) {

        int pageSize = 10;

        if (search != null && !search.isEmpty()) {
            // Search mode: Single list logic
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Page<Product> productPage = productService.searchProductsPaginated(search,
                    pageable);

            model.addAttribute("searchResults", productPage.getContent());
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentPage", activePage);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());
            model.addAttribute("isSearching", true);
        } else {
            // Normal mode: Split lists
            org.springframework.data.domain.Pageable activeRequest = org.springframework.data.domain.PageRequest
                    .of(activePage, pageSize);
            org.springframework.data.domain.Pageable inactiveRequest = org.springframework.data.domain.PageRequest
                    .of(inactivePage, pageSize);

            org.springframework.data.domain.Page<Product> activeProductPage = productService
                    .getProductsByStatusPaginated(true, activeRequest);
            org.springframework.data.domain.Page<Product> inactiveProductPage = productService
                    .getProductsByStatusPaginated(false, inactiveRequest);

            model.addAttribute("activeProducts", activeProductPage.getContent());
            model.addAttribute("activePage", activePage);
            model.addAttribute("totalActivePages", activeProductPage.getTotalPages());
            model.addAttribute("totalActiveCount", activeProductPage.getTotalElements());

            model.addAttribute("inactiveProducts", inactiveProductPage.getContent());
            model.addAttribute("inactivePage", inactivePage);
            model.addAttribute("totalInactivePages", inactiveProductPage.getTotalPages());
            model.addAttribute("totalInactiveCount", inactiveProductPage.getTotalElements());

            model.addAttribute("isSearching", false);
        }

        model.addAttribute("search", search);
        return "admin/products/index";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Product product = new Product();
        // Add default size row for usability
        product.setSizes(java.util.List.of(new ProductSize()));

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        addIngredientsToModel(model);
        return "admin/products/form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            RedirectAttributes ra) {
        // Remove empty sizes if any
        if (product.getSizes() != null) {
            product.getSizes().removeIf(size -> size.getSizeName() == null || size.getPrice() == null);
        }

        // Fix logic: Link recipes to product (Parent-Child relationship) for Cascade to
        // work
        if (product.getRecipes() != null) {
            product.getRecipes().removeIf(r -> r.getIngredient() == null || r.getIngredient().getId() == null
                    || r.getQuantityRequired() == null);
            product.getRecipes().forEach(recipe -> recipe.setProduct(product));
        }

        // Determine final image value
        String existingImage = null;
        if (product.getId() != null) {
            existingImage = productService.getProductById(product.getId())
                    .map(Product::getImage)
                    .orElse(null);
        }

        if (!imageFile.isEmpty()) {
            try {
                String uploadDir = "src/main/resources/static/images/products/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = java.util.UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                try (InputStream inputStream = imageFile.getInputStream()) {
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                    product.setImage("/images/products/" + fileName);
                }
            } catch (IOException e) {
                ra.addFlashAttribute("error", "Could not save image: " + e.getMessage());
                // Keep existing image on error
                if (existingImage != null) {
                    product.setImage(existingImage);
                }
            }
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            // Use the provided URL if no file is uploaded
            String url = imageUrl.trim();
            // Don't add https:// prefix to local paths (starting with /)
            if (!url.startsWith("/") && !url.toLowerCase().startsWith("http://")
                    && !url.toLowerCase().startsWith("https://")) {
                url = "https://" + url;
            }
            product.setImage(url);
        } else {
            // No new image provided (File or URL) - preserve existing
            // This covers both "imageUrl is explicitly empty" and "imageUrl is null"
            if (existingImage != null) {
                product.setImage(existingImage);
            }
        }

        productService.saveProduct(product);
        ra.addFlashAttribute("message", "Product saved successfully!");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getAllCategories());
            addIngredientsToModel(model);
            return "admin/products/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Product not found.");
            return "redirect:/admin/products";
        }
    }

    @GetMapping("/activate/{id}")
    public String activateProduct(@PathVariable Long id, RedirectAttributes ra) {
        productService.updateStatus(id, true);
        ra.addFlashAttribute("message", "Product reactivated successfully!");
        return "redirect:/admin/products";
    }

    @GetMapping("/deactivate/{id}")
    public String deactivateProduct(@PathVariable Long id, RedirectAttributes ra) {
        productService.updateStatus(id, false);
        ra.addFlashAttribute("message", "Product deactivated successfully!");
        return "redirect:/admin/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        productService.deleteProduct(id);
        ra.addFlashAttribute("message", "Product deleted successfully!");
        return "redirect:/admin/products";
    }

    /**
     * Helper method to add ingredients list and JSON to model for recipe dropdown.
     */
    private void addIngredientsToModel(Model model) {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        model.addAttribute("ingredients", ingredients);
        try {
            String ingredientsJson = objectMapper.writeValueAsString(ingredients);
            model.addAttribute("ingredientsJson", ingredientsJson);
        } catch (Exception e) {
            model.addAttribute("ingredientsJson", "[]");
        }
    }
}
