package com.example.expirytrack.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expirytrack.LoginActivity;
import com.example.expirytrack.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText displayNameInput;
    private EditText notifyDaysInput;
    private Switch darkModeSwitch;
    private Button saveButton;
    private Button logoutButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        displayNameInput = findViewById(R.id.displayNameInput);
        notifyDaysInput = findViewById(R.id.notifyDaysInput);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void loadSettings() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            displayNameInput.setText(user.getDisplayName());
        }

        int notifyDays = prefs.getInt("default_notify_days", 3);
        notifyDaysInput.setText(String.valueOf(notifyDays));

        boolean darkMode = prefs.getBoolean("dark_mode_enabled", false);
        darkModeSwitch.setChecked(darkMode);
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveSettings());
        logoutButton.setOnClickListener(v -> logout());
    }

    private void saveSettings() {
        String newDisplayName = displayNameInput.getText().toString().trim();
        String notifyDaysStr = notifyDaysInput.getText().toString().trim();

        if (newDisplayName.isEmpty()) {
            Toast.makeText(this, "กรุณาป้อนชื่อแสดง", Toast.LENGTH_SHORT).show();
            return;
        }

        if (notifyDaysStr.isEmpty()) {
            Toast.makeText(this, "กรุณาป้อนจำนวนวันแจ้งเตือน", Toast.LENGTH_SHORT).show();
            return;
        }

        int notifyDays = Integer.parseInt(notifyDaysStr);
        if (notifyDays < 0) {
            Toast.makeText(this, "จำนวนวันต้องมากกว่า 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("default_notify_days", notifyDays);
        editor.putBoolean("dark_mode_enabled", darkModeSwitch.isChecked());
        editor.apply();

        // Update Firebase user display name
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && !newDisplayName.equals(user.getDisplayName())) {
            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            user.updateProfile(profileUpdate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SettingsActivity.this, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SettingsActivity.this, "บันทึกข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void logout() {
        auth.signOut();
        startActivity(new android.content.Intent(SettingsActivity.this, LoginActivity.class)
                .setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
