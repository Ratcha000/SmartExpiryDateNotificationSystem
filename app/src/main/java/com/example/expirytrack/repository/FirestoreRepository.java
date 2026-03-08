// FirestoreRepository.java
package com.example.expirytrack.repository;

import android.util.Log;

import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.model.Restaurant;
import com.example.expirytrack.model.UsageHistory;
import com.example.expirytrack.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class FirestoreRepository {
    private static final String TAG = "FirestoreRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // ─────────────────────────── USER ───────────────────────────

    /** Create user document in /users/{uid} */
    public void createUser(String uid, String email, String displayName,
            String role, String restaurantId, OnCompleteListener listener) {
        User user = new User(uid, email, displayName, role, restaurantId);
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created: " + uid);
                    listener.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    /** Fetch a single user document */
    public void getUser(String uid, OnUserFetchedListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        listener.onUserFetched(true, doc.toObject(User.class), null);
                    } else {
                        listener.onUserFetched(false, null, "User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user", e);
                    listener.onUserFetched(false, null, e.getMessage());
                });
    }

    // ─────────────────────────── RESTAURANT ───────────────────────────

    /** Create a new restaurant and return its generated ID */
    public void createRestaurant(String restaurantName, String managerId,
            String inviteCode, OnRestaurantCreatedListener listener) {
        String restaurantId = db.collection("restaurants").document().getId();
        Restaurant restaurant = new Restaurant(restaurantId, restaurantName,
                managerId, inviteCode, FieldValue.serverTimestamp());

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

    /** Find a restaurant by its invite code */
    public void findRestaurantByCode(String inviteCode, OnRestaurantFoundListener listener) {
        db.collection("restaurants").whereEqualTo("inviteCode", inviteCode).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        Restaurant r = snapshot.getDocuments().get(0).toObject(Restaurant.class);
                        listener.onRestaurantFound(true, r, null);
                    } else {
                        listener.onRestaurantFound(false, null, "Invite code not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding restaurant", e);
                    listener.onRestaurantFound(false, null, e.getMessage());
                });
    }

    // ─────────────────────────── INGREDIENTS ───────────────────────────

    /**
     * Add a new ingredient to Firestore.
     * The document ID is auto-generated and written back into ingredient.id.
     */
    public void addIngredient(Ingredient ingredient, OnCompleteListener listener) {
        db.collection("ingredients").add(ingredient)
                .addOnSuccessListener(ref -> {
                    // Write the auto-generated ID back into the document
                    ingredient.setId(ref.getId());
                    db.collection("ingredients").document(ref.getId())
                            .update("id", ref.getId())
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Ingredient added: " + ref.getId());
                                listener.onComplete(true, null);
                            })
                            .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding ingredient", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    /**
     * Update an existing ingredient (full document replace).
     * ingredient.getId() must be set.
     */
    public void updateIngredient(Ingredient ingredient, OnCompleteListener listener) {
        if (ingredient.getId() == null || ingredient.getId().isEmpty()) {
            listener.onComplete(false, "Ingredient ID is null");
            return;
        }
        ingredient.setUpdatedAt(System.currentTimeMillis());
        db.collection("ingredients").document(ingredient.getId()).set(ingredient)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ingredient updated: " + ingredient.getId());
                    listener.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating ingredient", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    /**
     * Soft-delete: sets status to "deleted" instead of removing the document.
     * Pass the performing user's UID for audit purposes.
     */
    public void deleteIngredient(String ingredientId, String performedByUid,
            OnCompleteListener listener) {
        db.collection("ingredients").document(ingredientId)
                .update("status", "deleted",
                        "updatedBy", performedByUid,
                        "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ingredient soft-deleted: " + ingredientId);
                    listener.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting ingredient", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    /**
     * Mark ingredient status as "used".
     */
    public void markIngredientUsed(String ingredientId, String performedByUid,
            OnCompleteListener listener) {
        db.collection("ingredients").document(ingredientId)
                .update("status", "used",
                        "updatedBy", performedByUid,
                        "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> listener.onComplete(true, null))
                .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
    }

    /**
     * Fetch all active ingredients for a restaurant (one-time get).
     * For real-time updates use addSnapshotListener directly on the query.
     */
    public void getIngredients(String restaurantId, OnIngredientsFetchedListener listener) {
        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .orderBy("expiryDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ingredient> list = snapshot.toObjects(Ingredient.class);
                    listener.onIngredientsFetched(true, list, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ingredients", e);
                    listener.onIngredientsFetched(false, null, e.getMessage());
                });
    }

    // ─────────────────────────── USAGE HISTORY ───────────────────────────

    /**
     * Append a UsageHistory record to /usageHistory collection.
     */
    public void addUsageHistory(UsageHistory history, OnCompleteListener listener) {
        db.collection("usageHistory").add(history)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "UsageHistory added: " + ref.getId());
                    listener.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding usage history", e);
                    listener.onComplete(false, e.getMessage());
                });
    }

    // ─────────────────────────── INVITE CODE ───────────────────────────

    /** Generate a random 6-character alphanumeric invite code */
    public static String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    // ─────────────────────────── LISTENER INTERFACES ───────────────────────────

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

    public interface OnIngredientsFetchedListener {
        void onIngredientsFetched(boolean success, List<Ingredient> ingredients, String error);
    }
}
