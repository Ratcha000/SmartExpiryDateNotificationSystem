package com.example.expirytrack;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.widget.Button;
import android.widget.TextView;

import com.example.expirytrack.repository.FirestoreRepository;
import com.example.expirytrack.util.NotificationHelper;
import com.example.expirytrack.worker.ExpiryCheckWorker;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private TextView welcomeText;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirestoreRepository firestoreRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firestoreRepo = new FirestoreRepository();

        welcomeText = findViewById(R.id.welcome_text);
        logoutButton = findViewById(R.id.logout_button);

        // Create notification channels
        NotificationHelper.createNotificationChannels(this);

        // Schedule daily expiry check
        scheduleExpiryCheck();

        String uid = auth.getCurrentUser().getUid();
        firestoreRepo.getUser(uid, (success, user, error) -> {
            if (success && user != null) {
                welcomeText.setText("Welcome, " + user.getDisplayName());
            }
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void scheduleExpiryCheck() {
        PeriodicWorkRequest expiryCheckRequest = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class,
                1,
                TimeUnit.DAYS).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "expiry_check",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                expiryCheckRequest);
    }
}
