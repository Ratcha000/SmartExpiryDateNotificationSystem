package com.example.expirytrack.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.expirytrack.R;

/**
 * Helper class for managing notifications
 */
public class NotificationHelper {
    public static final String EXPIRY_CHANNEL_ID = "expiry_alerts";
    private static final String EXPIRY_CHANNEL_NAME = "การแจ้งเตือนวันหมดอายุ";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                // Create Expiry Alerts Channel
                NotificationChannel channel = new NotificationChannel(
                        EXPIRY_CHANNEL_ID,
                        EXPIRY_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("แจ้งเตือนเมื่อวัตถุดิบใกล้หมดอายุ");
                channel.enableLights(true);
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendExpiryNotification(Context context, String title, String body, int notificationId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EXPIRY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    public static void sendGroupNotification(Context context, String title, String body, int groupNotificationId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EXPIRY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup("expiry_group")
                .setGroupSummary(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (notificationManager != null) {
            notificationManager.notify(groupNotificationId, builder.build());
        }
    }
}
