package com.coffeeshop.controller;

import com.coffeeshop.dto.Cart;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.User;
import com.coffeeshop.service.CartService;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService; // Need to create UserService lookup by username if not managing user in
                                           // session

    @GetMapping
    public String checkout(HttpSession session, Model model, Authentication authentication) {
        Cart cart = cartService.getCart(session);
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);

        // Pre-fill if logged in
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userService.findByUsername(username).ifPresent(user -> {
                model.addAttribute("user", user);
            });
        }

        return "checkout/index";
    }

    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam("customerName") String customerName,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam("note") String note,
            HttpSession session,
            Authentication authentication,
            Model model) {

        Cart cart = cartService.getCart(session);
        if (cart.getItems().isEmpty()) {
            return "redirect:/";
        }

        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            if (userOpt.isPresent())
                user = userOpt.get();
        }

        Order order = orderService.placeOrder(cart, user, customerName, phone, address, note);

        // Clear cart
        cartService.clearCart(session);

        model.addAttribute("order", order);
        return "checkout/success";
    }
}
