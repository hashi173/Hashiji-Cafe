package com.coffeeshop.controller;

import com.coffeeshop.dto.CartItem;
import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductSize;
import com.coffeeshop.entity.Topping;
import com.coffeeshop.service.CartService;
import com.coffeeshop.service.ProductService;
import com.coffeeshop.service.ToppingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ProductService productService;
    private final ToppingService toppingService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCart(session));
        return "cart/index";
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam("productId") Long productId,
            @RequestParam("sizeId") Long sizeId,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "toppingIds", required = false) List<Long> toppingIds,
            @RequestParam(value = "sugar", defaultValue = "100%") String sugar,
            @RequestParam(value = "ice", defaultValue = "100%") String ice,
            @RequestParam("note") String note,
            HttpSession session) {

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product"));

        // Find Size info
        ProductSize selectedSize = product.getSizes().stream()
                .filter(s -> s.getId().equals(sizeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid size"));

        // Fetch toppings
        List<Topping> toppings = new ArrayList<>();
        List<String> toppingNames = new ArrayList<>();
        double toppingsPrice = 0.0;

        if (toppingIds != null && !toppingIds.isEmpty()) {
            List<Topping> allToppings = toppingService.getAllToppings(); // Ideally filter by IDs in DB
            for (Long tId : toppingIds) {
                allToppings.stream().filter(t -> t.getId().equals(tId)).findFirst().ifPresent(t -> {
                    toppings.add(t);
                    toppingNames.add(t.getName());
                });
            }
            toppingsPrice = toppings.stream().mapToDouble(Topping::getPrice).sum();
        }

        // Calculate unit price (Size Price + Toppings)
        double unitPrice = selectedSize.getPrice() + toppingsPrice;

        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductNameVi(product.getNameVi());
        String img = product.getImage();
        if (img == null || img.isEmpty()) {
            img = "/images/no-image.png";
        } else if (!img.startsWith("http") && !img.startsWith("/")) {
            img = "/images/products/" + img;
        }
        item.setProductImage(img);
        item.setSizeId(sizeId);
        item.setSizeName(selectedSize.getSizeName());
        item.setPrice(unitPrice);
        item.setQuantity(quantity);
        // Append Sugar and Ice to toppingNames for display
        if (!"100%".equals(sugar)) {
            toppingNames.add("Sugar " + sugar);
        }
        if (!"100%".equals(ice)) {
            toppingNames.add("Ice " + ice);
        }

        item.setToppingIds(toppingIds != null ? toppingIds : new ArrayList<>());
        item.setToppingNames(toppingNames);
        item.setNote(note);

        cartService.addItemToCart(session, item);

        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam("index") int index, @RequestParam("quantity") int quantity,
            HttpSession session) {
        cartService.updateCartItem(session, index, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/remove/{index}")
    public String removeItem(@PathVariable("index") int index, HttpSession session) {
        cartService.removeCartItem(session, index);
        return "redirect:/cart";
    }
}
