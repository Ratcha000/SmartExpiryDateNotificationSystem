package com.example.expirytrack.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.util.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker to check for expiring ingredients and send notifications.
 * Uses CountDownLatch to properly await async Firebase calls before returning.
 */
public class ExpiryCheckWorker extends Worker {
    private static final String TAG = "ExpiryCheckWorker";
    private final FirebaseFirestore db;
    private static final int NOTIFICATION_ID_BASE = 1000;
    // Timeout for Firestore operations (30 seconds)
    private static final long FIRESTORE_TIMEOUT_SECONDS = 30;

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ExpiryCheckWorker started");

        // CountDownLatch to block until all restaurant checks complete
        CountDownLatch rootLatch = new CountDownLatch(1);
        AtomicBoolean workerSuccess = new AtomicBoolean(true);

        db.collection("restaurants").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No restaurants found");
                        rootLatch.countDown();
                        return;
                    }

                    List<com.google.firebase.firestore.DocumentSnapshot> docs = querySnapshot.getDocuments();
                    int total = docs.size();
                    CountDownLatch restaurantLatch = new CountDownLatch(total);

                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        String restaurantId = doc.getId();
                        checkExpiringIngredientsForRestaurant(restaurantId, restaurantLatch);
                    }

                    // Wait for all restaurant checks to finish (max 25 seconds)
                    try {
                        restaurantLatch.await(25, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for restaurant checks", e);
                        workerSuccess.set(false);
                    }
                    rootLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch restaurants", e);
                    workerSuccess.set(false);
                    rootLatch.countDown();
                });

        // Block the Worker thread until all async work finishes
        try {
            boolean completed = rootLatch.await(FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                Log.w(TAG, "Worker timed out waiting for Firestore");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.retry();
        }

        Log.d(TAG, "ExpiryCheckWorker finished — success=" + workerSuccess.get());
        return workerSuccess.get() ? Result.success() : Result.retry();
    }

    /**
     * Queries ingredients for one restaurant and sends notifications.
     * Calls latch.countDown() when done (success OR failure).
     */
    private void checkExpiringIngredientsForRestaurant(String restaurantId, CountDownLatch latch) {
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
                                    expiredItems.add(ingredient);
                                } else if (daysLeft <= ingredient.getNotifyDaysBefore()) {
                                    expiringItems.add(ingredient);
                                }
                            }
                        }
                        sendNotifications(restaurantId, expiringItems, expiredItems);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch ingredients for restaurant: " + restaurantId, e);
                    latch.countDown();
                });
    }

    private void sendNotifications(String restaurantId,
            List<Ingredient> expiringItems,
            List<Ingredient> expiredItems) {

        int notificationId = NOTIFICATION_ID_BASE + Math.abs(restaurantId.hashCode() % 500);

        // Send individual expired notifications
        for (Ingredient item : expiredItems) {
            String title = "❌ วัตถุดิบหมดอายุแล้ว";
            String body = item.getName() + " หมดอายุแล้ว กรุณาตรวจสอบ";
            NotificationHelper.sendExpiryNotification(getApplicationContext(), title, body, notificationId++);
        }

        // Send expiring-soon notifications
        if (!expiringItems.isEmpty()) {
            if (expiringItems.size() == 1) {
                Ingredient item = expiringItems.get(0);
                long daysLeft = calculateDaysUntilExpiry(item.getExpiryDate());
                String title = "⚠️ วัตถุดิบใกล้หมดอายุ";
                String body = item.getName() + " — เหลืออีก " + daysLeft + " วัน";
                NotificationHelper.sendExpiryNotification(getApplicationContext(), title, body, notificationId++);
            } else {
                // Group summary notification
                String title = "⚠️ มี " + expiringItems.size() + " รายการใกล้หมดอายุ";
                String body = "กดเพื่อดูรายละเอียด";
                NotificationHelper.sendGroupNotification(getApplicationContext(), title, body, notificationId);

                // Individual notifications
                for (Ingredient item : expiringItems) {
                    long daysLeft = calculateDaysUntilExpiry(item.getExpiryDate());
                    String itemBody = item.getName() + " — เหลืออีก " + daysLeft + " วัน";
                    NotificationHelper.sendExpiryNotification(getApplicationContext(), title, itemBody, notificationId++);
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
