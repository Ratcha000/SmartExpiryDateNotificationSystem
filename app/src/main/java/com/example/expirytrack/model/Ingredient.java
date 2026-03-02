package com.example.expirytrack.model;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String id;
    private String restaurantId;
    private String name;
    private String category;
    private long expiryDate;
    private int notifyDaysBefore;
    private String status; // "active", "used", "deleted", "expired"
    private String scannedBy;
    private long scannedAt;
    private String updatedBy;
    private long createdAt;
    private long updatedAt;

    public Ingredient() {
    }

    public Ingredient(String name, String category, long expiryDate, int notifyDaysBefore,
            String restaurantId) {
        this.name = name;
        this.category = category;
        this.expiryDate = expiryDate;
        this.notifyDaysBefore = notifyDaysBefore;
        this.restaurantId = restaurantId;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getNotifyDaysBefore() {
        return notifyDaysBefore;
    }

    public void setNotifyDaysBefore(int notifyDaysBefore) {
        this.notifyDaysBefore = notifyDaysBefore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(long scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
