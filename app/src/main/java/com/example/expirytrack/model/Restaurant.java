// Restaurant.java
package com.example.expirytrack.model;

import com.google.firebase.firestore.FieldValue;

public class Restaurant {
    private String id;
    private String name;
    private String managerId;
    private String inviteCode;
    private Object createdAt;

    public Restaurant() {}

    public Restaurant(String id, String name, String managerId, String inviteCode, Object createdAt) {
        this.id = id;
        this.name = name;
        this.managerId = managerId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
}
