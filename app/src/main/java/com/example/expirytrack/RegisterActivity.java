// RegisterActivity.java
package com.example.expirytrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.expirytrack.activity.EmployeeMainActivity;
import com.example.expirytrack.activity.ManagerMainActivity;
import com.example.expirytrack.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput, nameInput, restaurantNameInput, inviteCodeInput;
    private RadioGroup roleGroup;
    private Button registerButton;
    private FirebaseAuth auth;
    private FirestoreRepository firestoreRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestoreRepo = new FirestoreRepository();

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        nameInput = findViewById(R.id.name_input);
        roleGroup = findViewById(R.id.role_group);
        restaurantNameInput = findViewById(R.id.restaurant_name_input);
        inviteCodeInput = findViewById(R.id.invite_code_input);
        registerButton = findViewById(R.id.register_button);

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> updateFormUI());
        registerButton.setOnClickListener(v -> registerUser());

        updateFormUI();
    }

    private void updateFormUI() {
        int selectedRole = roleGroup.getCheckedRadioButtonId();
        if (selectedRole == R.id.role_manager) {
            restaurantNameInput.setVisibility(android.view.View.VISIBLE);
            inviteCodeInput.setVisibility(android.view.View.GONE);
        } else {
            restaurantNameInput.setVisibility(android.view.View.GONE);
            inviteCodeInput.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRole = roleGroup.getCheckedRadioButtonId();
        String role = selectedRole == R.id.role_manager ? "manager" : "employee";

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    if (role.equals("manager")) {
                        String restaurantName = restaurantNameInput.getText().toString().trim();
                        String inviteCode = FirestoreRepository.generateInviteCode();

                        firestoreRepo.createRestaurant(restaurantName, uid, inviteCode,
                                (success, restaurantId, error) -> {
                                    if (success) {
                                        firestoreRepo.createUser(uid, email, name, role, restaurantId,
                                                (userSuccess, userError) -> {
                                                    if (userSuccess) {
                                                        Toast.makeText(RegisterActivity.this,
                                                                "Restaurant created! Code: " + inviteCode,
                                                                Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(RegisterActivity.this,
                                                                ManagerMainActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Error: " + userError,
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                    } else {
                        String inviteCode = inviteCodeInput.getText().toString().trim();
                        if (inviteCode.isEmpty()) {
                            Toast.makeText(this, "Please enter invite code", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        firestoreRepo.findRestaurantByCode(inviteCode, (success, restaurant, error) -> {
                            if (success && restaurant != null) {
                                firestoreRepo.createUser(uid, email, name, role, restaurant.getId(),
                                        (userSuccess, userError) -> {
                                            if (userSuccess) {
                                                Toast.makeText(RegisterActivity.this, "Joined restaurant successfully",
                                                        Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(RegisterActivity.this,
                                                        EmployeeMainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Error: " + userError,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(RegisterActivity.this, "Invalid invite code", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                        .show());
    }
}
