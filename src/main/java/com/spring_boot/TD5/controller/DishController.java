package com.spring_boot.TD5.controller;

import com.spring_boot.TD5.entity.*;
import com.spring_boot.TD5.repository.DishRepository;
import com.spring_boot.TD5.repository.IngredientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DishController {

    private final DishRepository dishRepository = new DishRepository();
    private final IngredientRepository ingredientRepository = new IngredientRepository();

    @GetMapping("/dishes")
    public ResponseEntity<List<Dish>> getDishes(
            @RequestParam(required = false) Double priceUnder,
            @RequestParam(required = false) Double priceOver,
            @RequestParam(required = false) String name) {

        List<Dish> dishes = dishRepository.findAll();

        if (priceUnder != null) {
            dishes = dishes.stream().filter(d -> d.getPrice() < priceUnder).toList();
        }
        if (priceOver != null) {
            dishes = dishes.stream().filter(d -> d.getPrice() > priceOver).toList();
        }
        if (name != null && !name.isBlank()) {
            dishes = dishes.stream()
                    .filter(d -> d.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
        }

        return ResponseEntity.ok(dishes);
    }

    @PutMapping("/dishes/{id}/ingredients")
    public ResponseEntity<?> updateDishIngredients(
            @PathVariable Integer id,
            @RequestBody(required = false) List<DishIngredient> ingredients
    ) {
        if (ingredients == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Request body is required");
        }

        Dish dish = dishRepository.findDishById(id);
        if (dish == null) {
            return ResponseEntity
                    .status(404)
                    .body("Dish.id=" + id + " is not found");
        }

        List<DishIngredient> validIngredients = new ArrayList<>();
        for (DishIngredient di : ingredients) {
            try {
                Ingredient ingredient = ingredientRepository.findIngredientById(di.getIngredient().getId());
                DishIngredient association = new DishIngredient();
                association.setIngredient(ingredient);
                association.setQuantity(di.getQuantity());
                association.setUnit(di.getUnit());
                association.setDish(dish);

                validIngredients.add(association);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        dish.setDishIngredients(validIngredients);
        Dish updatedDish = dishRepository.saveDish(dish);

        return ResponseEntity.ok(updatedDish);
    }

    @PostMapping("/dishes")
    public ResponseEntity<?> createDishes(@RequestBody List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return ResponseEntity.badRequest().body("Request body cannot be empty");
        }

        List<Dish> createdDishes = new ArrayList<>();

        try {
            for (Dish d : dishes) {
                try {
                    Dish existing = dishRepository.findDishByName(d.getName());
                    if (existing != null) {
                        return ResponseEntity
                                .badRequest()
                                .body("Dish.name=" + d.getName() + " already exists");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Dish newDish = new Dish();
                newDish.setName(d.getName());
                newDish.setDishType(d.getDishType());
                newDish.setPrice(d.getPrice());

                Dish saved = dishRepository.saveDish(newDish);
                createdDishes.add(saved);
            }

            return ResponseEntity.status(201).body(createdDishes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while creating dishes");
        }
    }
}