// SplashActivity.java
package com.example.expirytrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.expirytrack.activity.EmployeeMainActivity;
import com.example.expirytrack.activity.ManagerMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserAndRedirect();
        }, 2000);
    }

    private void checkUserAndRedirect() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // No user logged in
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // User logged in - fetch role from Firestore
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            Intent intent;

                            if ("employee".equalsIgnoreCase(role)) {
                                intent = new Intent(SplashActivity.this, EmployeeMainActivity.class);
                            } else if ("manager".equalsIgnoreCase(role)) {
                                intent = new Intent(SplashActivity.this, ManagerMainActivity.class);
                            } else {
                                // Unknown role - go to login
                                intent = new Intent(SplashActivity.this, LoginActivity.class);
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // Document doesn't exist - go to login
                            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Error fetching user - go to login
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
        }
    }
}
