package com.coffeeshop.service;

import com.coffeeshop.entity.Topping;
import com.coffeeshop.repository.ToppingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToppingService {

    private final ToppingRepository toppingRepository;

    public List<Topping> getAllToppings() {
        return toppingRepository.findAll();
    }
}
