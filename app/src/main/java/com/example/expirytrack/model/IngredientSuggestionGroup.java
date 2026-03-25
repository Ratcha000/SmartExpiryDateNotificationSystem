package com.example.expirytrack.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one accordion group in the Suggestions screen.
 * Each group = one near-expiry ingredient with its AI-generated menu suggestions.
 */
public class IngredientSuggestionGroup {
    private String ingredientName;
    private int daysLeft;
    private String expiryDateFormatted; // e.g. "28/03/2026"
    private List<MenuSuggestion> menus = new ArrayList<>();

    // UI state (not persisted)
    private boolean isExpanded = false;
    private boolean isLoading = false;
    private boolean hasLoaded = false; // true once Gemini result is cached

    public IngredientSuggestionGroup() {}

    public IngredientSuggestionGroup(String ingredientName, int daysLeft, String expiryDateFormatted) {
        this.ingredientName = ingredientName;
        this.daysLeft = daysLeft;
        this.expiryDateFormatted = expiryDateFormatted;
    }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public int getDaysLeft() { return daysLeft; }
    public void setDaysLeft(int daysLeft) { this.daysLeft = daysLeft; }

    public String getExpiryDateFormatted() { return expiryDateFormatted; }
    public void setExpiryDateFormatted(String expiryDateFormatted) {
        this.expiryDateFormatted = expiryDateFormatted;
    }

    public List<MenuSuggestion> getMenus() { return menus; }
    public void setMenus(List<MenuSuggestion> menus) { this.menus = menus; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public boolean isHasLoaded() { return hasLoaded; }
    public void setHasLoaded(boolean hasLoaded) { this.hasLoaded = hasLoaded; }
}
