package com.example.expirytrack.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.expirytrack.R;
import com.example.expirytrack.activity.SettingsActivity;
import com.example.expirytrack.fragment.HomeFragment;
import com.example.expirytrack.fragment.ProfileFragment;
import com.example.expirytrack.fragment.ScanFragment;
import com.example.expirytrack.fragment.SuggestionsFragment;
import com.example.expirytrack.util.NotificationHelper;
import com.example.expirytrack.worker.ExpiryCheckWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.concurrent.TimeUnit;

public class EmployeeMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Enable offline persistence
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        db.setFirestoreSettings(settings);

        // Create notification channels
        NotificationHelper.createNotificationChannels(this);

        // Schedule daily expiry check
        scheduleExpiryCheck();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), fragmentManager);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_scan) {
                fragment = new ScanFragment();
            } else if (item.getItemId() == R.id.nav_suggestions) {
                fragment = new SuggestionsFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }
            if (fragment != null) {
                loadFragment(fragment, fragmentManager);
            }
            return true;
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_employee_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment, FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
