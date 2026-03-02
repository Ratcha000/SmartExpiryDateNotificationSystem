package com.example.expirytrack.model;

import java.util.Date;

public class UsageHistory {
    private String id;
    private String ingredientId;
    private String restaurantId;
    private String ingredientName;
    private String action; // "used", "deleted", "expired"
    private String performedBy; // userId
    private String performedByName; // username
    private Date date; // Date object

    public UsageHistory() {
    }

    public UsageHistory(String ingredientId, String restaurantId, String ingredientName,
            String action, String performedBy, String performedByName, Date date) {
        this.ingredientId = ingredientId;
        this.restaurantId = restaurantId;
        this.ingredientName = ingredientName;
        this.action = action;
        this.performedBy = performedBy;
        this.performedByName = performedByName;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByName() {
        return performedByName;
    }

    public void setPerformedByName(String performedByName) {
        this.performedByName = performedByName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
