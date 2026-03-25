package com.example.expirytrack.model;

import java.util.List;

/**
 * Represents a menu suggestion returned by Gemini AI.
 */
public class MenuSuggestion {
    private String menuName;
    private List<String> ingredients;       // All ingredients needed
    private List<String> ingredientsInStock; // Subset that are already in the restaurant
    private List<String> steps;             // Cooking steps

    public MenuSuggestion() {}

    public MenuSuggestion(String menuName, List<String> ingredients,
            List<String> ingredientsInStock, List<String> steps) {
        this.menuName = menuName;
        this.ingredients = ingredients;
        this.ingredientsInStock = ingredientsInStock;
        this.steps = steps;
    }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getIngredientsInStock() { return ingredientsInStock; }
    public void setIngredientsInStock(List<String> ingredientsInStock) {
        this.ingredientsInStock = ingredientsInStock;
    }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
}
