// FirestoreRepository.java
package com.example.expirytrack.repository;

import android.util.Log;
import com.example.expirytrack.model.Restaurant;
import com.example.expirytrack.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class FirestoreRepository {
    private static final String TAG = "FirestoreRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Create user in Firestore
    public void createUser(String uid, String email, String displayName, String role, String restaurantId, OnCompleteListener listener) {
        User user = new User(uid, email, displayName, role, restaurantId);
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created successfully");
                    listener.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    // Create restaurant
    public void createRestaurant(String restaurantName, String managerId, String inviteCode, OnRestaurantCreatedListener listener) {
        String restaurantId = db.collection("restaurants").document().getId();
        Restaurant restaurant = new Restaurant(restaurantId, restaurantName, managerId, inviteCode, FieldValue.serverTimestamp());

        db.collection("restaurants").document(restaurantId).set(restaurant)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Restaurant created: " + restaurantId);
                    listener.onRestaurantCreated(true, restaurantId, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating restaurant", e);
                    listener.onRestaurantCreated(false, null, e.getMessage());
                });
    }

    // Find restaurant by invite code
    public void findRestaurantByCode(String inviteCode, OnRestaurantFoundListener listener) {
        db.collection("restaurants").whereEqualTo("inviteCode", inviteCode).limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Restaurant restaurant = querySnapshot.getDocuments().get(0).toObject(Restaurant.class);
                        listener.onRestaurantFound(true, restaurant, null);
                    } else {
                        listener.onRestaurantFound(false, null, "Invite code not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding restaurant", e);
                    listener.onRestaurantFound(false, null, e.getMessage());
                });
    }

    // Get user
    public void getUser(String uid, OnUserFetchedListener listener) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserFetched(true, user, null);
                    } else {
                        listener.onUserFetched(false, null, "User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user", e);
                    listener.onUserFetched(false, null, e.getMessage());
                });
    }

    // Generate 6-character invite code
    public static String generateInviteCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return code.toString();
    }

    // Listeners
    public interface OnCompleteListener {
        void onComplete(boolean success, String error);
    }

    public interface OnRestaurantCreatedListener {
        void onRestaurantCreated(boolean success, String restaurantId, String error);
    }

    public interface OnRestaurantFoundListener {
        void onRestaurantFound(boolean success, Restaurant restaurant, String error);
    }

    public interface OnUserFetchedListener {
        void onUserFetched(boolean success, User user, String error);
    }
}

