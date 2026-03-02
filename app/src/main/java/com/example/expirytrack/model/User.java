// User.java
package com.example.expirytrack.model;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String role; // "manager" or "employee"
    private String restaurantId;

    public User() {}

    public User(String uid, String email, String displayName, String role, String restaurantId) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.restaurantId = restaurantId;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
}
