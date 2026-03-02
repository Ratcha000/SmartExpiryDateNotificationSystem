package com.example.expirytrack.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.util.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker to check for expiring ingredients and send notifications
 * Runs daily at 8:00 AM
 */
public class ExpiryCheckWorker extends Worker {
    private FirebaseFirestore db;
    private static final int NOTIFICATION_ID_BASE = 1000;

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Get all restaurants from Firestore
            db.collection("restaurants").get().addOnSuccessListener(querySnapshot -> {
                if (querySnapshot != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String restaurantId = doc.getId();
                        checkExpiringIngredientsForRestaurant(restaurantId);
                    }
                }
            });

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void checkExpiringIngredientsForRestaurant(String restaurantId) {
        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<Ingredient> expiringItems = new ArrayList<>();
                        List<Ingredient> expiredItems = new ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Ingredient ingredient = doc.toObject(Ingredient.class);
                            if (ingredient != null) {
                                ingredient.setId(doc.getId());
                                long daysLeft = calculateDaysUntilExpiry(ingredient.getExpiryDate());

                                if (daysLeft < 0) {
                                    // Expired
                                    expiredItems.add(ingredient);
                                } else if (daysLeft <= ingredient.getNotifyDaysBefore()) {
                                    // Expiring soon
                                    expiringItems.add(ingredient);
                                }
                            }
                        }

                        // Send notifications
                        sendNotifications(restaurantId, expiringItems, expiredItems);
                    }
                });
    }

    private void sendNotifications(String restaurantId, List<Ingredient> expiringItems, List<Ingredient> expiredItems) {
        int notificationId = NOTIFICATION_ID_BASE + restaurantId.hashCode();

        if (!expiredItems.isEmpty()) {
            for (Ingredient item : expiredItems) {
                String title = "❌ วัตถุดิบหมดอายุแล้ว";
                String body = item.getName() + " หมดอายุแล้ว";
                NotificationHelper.sendExpiryNotification(getApplicationContext(), title, body, notificationId++);
            }
        }

        if (!expiringItems.isEmpty()) {
            if (expiringItems.size() == 1) {
                Ingredient item = expiringItems.get(0);
                long daysLeft = calculateDaysUntilExpiry(item.getExpiryDate());
                String title = "⚠️ วัตถุดิบใกล้หมดอายุ";
                String body = item.getName() + " — เหลืออีก " + daysLeft + " วัน";
                NotificationHelper.sendExpiryNotification(getApplicationContext(), title, body, notificationId++);
            } else {
                String title = "⚠️ มี " + expiringItems.size() + " รายการใกล้หมดอายุ";
                String body = "กดเพื่อดูรายละเอียด";
                NotificationHelper.sendGroupNotification(getApplicationContext(), title, body, notificationId);

                // Send individual notifications for each item
                for (Ingredient item : expiringItems) {
                    long daysLeft = calculateDaysUntilExpiry(item.getExpiryDate());
                    String itemBody = item.getName() + " — เหลืออีก " + daysLeft + " วัน";
                    NotificationHelper.sendExpiryNotification(getApplicationContext(), title, itemBody,
                            notificationId++);
                }
            }
        }
    }

    private long calculateDaysUntilExpiry(long expiryDate) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar expiry = Calendar.getInstance();
        expiry.setTimeInMillis(expiryDate);
        expiry.set(Calendar.HOUR_OF_DAY, 0);
        expiry.set(Calendar.MINUTE, 0);
        expiry.set(Calendar.SECOND, 0);
        expiry.set(Calendar.MILLISECOND, 0);

        return (expiry.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);
    }
}
