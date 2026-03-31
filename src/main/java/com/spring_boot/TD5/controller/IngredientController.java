package com.spring_boot.TD5.controller;

import com.spring_boot.TD5.entity.*;
import com.spring_boot.TD5.repository.IngredientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository = new IngredientRepository();

    @GetMapping
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIngredientById(@PathVariable Integer id) {
        try {
            Ingredient ingredient = ingredientRepository.findIngredientById(id);
            return ResponseEntity.ok(ingredient);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body("Ingredient.id=" + id + " is not found");
        }
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStock(
            @PathVariable Integer id,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) String unit
    ) {
        if (at == null || unit == null) {
            return ResponseEntity.badRequest()
                    .body("Either mandatory query parameter `at` or `unit` is not provided.");
        }

        try {
            Instant instant = Instant.parse(at);
            StockValue stock = ingredientRepository.getStockValueAt(instant, id);
            return ResponseEntity.ok(stock);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body("Ingredient.id=" + id + " is not found");
        }
    }
}