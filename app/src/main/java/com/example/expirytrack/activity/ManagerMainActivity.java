package com.example.expirytrack.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.expirytrack.LoginActivity;
import com.example.expirytrack.R;
import com.example.expirytrack.fragment.DashboardFragment;
import com.example.expirytrack.fragment.HomeFragment;
import com.example.expirytrack.fragment.ScanFragment;
import com.example.expirytrack.fragment.SuggestionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ManagerMainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_main);

        auth = FirebaseAuth.getInstance();
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // Set up bottom navigation
        setupBottomNavigation();

        // Load dashboard fragment by default
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.nav_scan) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, new ScanFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.nav_suggestions) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, new SuggestionsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(ManagerMainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
