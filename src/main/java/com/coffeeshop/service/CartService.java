package com.coffeeshop.service;

import com.coffeeshop.dto.Cart;
import com.coffeeshop.dto.CartItem;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_SESSION_KEY = "cart";

    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public void addItemToCart(HttpSession session, CartItem newItem) {
        Cart cart = getCart(session);
        cart.addItem(newItem);
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void updateCartItem(HttpSession session, int index, int quantity) {
        Cart cart = getCart(session);
        cart.updateQuantity(index, quantity);
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void removeCartItem(HttpSession session, int index) {
        Cart cart = getCart(session);
        cart.removeItem(index);
        session.setAttribute(CART_SESSION_KEY, cart);
    }
    
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }
}
