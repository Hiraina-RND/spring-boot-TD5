package com.spring_boot.TD5;

import com.spring_boot.TD5.repository.DishRepository;
import com.spring_boot.TD5.repository.IngredientRepository;

public class Main {
    public static void main(String[] args) {
        DishRepository dishRepository = new DishRepository();
        IngredientRepository ingredientRepository = new IngredientRepository();
        System.out.println(dishRepository.findAll());
        System.out.println("==================");
        System.out.println(ingredientRepository.findIngredientByDishId(1));
        System.out.println("===================");
        System.out.println(dishRepository.findDishByName("Salade de fruits"));
    }
}
