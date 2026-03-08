package com.example.expirytrack.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expirytrack.LoginActivity;
import com.example.expirytrack.R;
import com.example.expirytrack.activity.SettingsActivity;
import com.example.expirytrack.model.Restaurant;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView profileImage;
    private TextView nameText;
    private TextView emailText;
    private TextView roleText;
    private TextView restaurantText;
    private MaterialButton settingsButton;
    private MaterialButton logoutButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadUserInfo();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        roleText = view.findViewById(R.id.roleText);
        restaurantText = view.findViewById(R.id.restaurantText);
        settingsButton = view.findViewById(R.id.settingsButton);
        logoutButton = view.findViewById(R.id.logoutButton);
    }

    private void loadUserInfo() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            String email = auth.getCurrentUser().getEmail();

            emailText.setText(email);

            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    com.example.expirytrack.model.User user = documentSnapshot
                            .toObject(com.example.expirytrack.model.User.class);
                    if (user != null) {
                        nameText.setText(user.getDisplayName());
                        roleText.setText(user.getRole().equals("manager") ? "ผู้จัดการ" : "พนักงาน");

                        // Load restaurant name
                        if (user.getRestaurantId() != null) {
                            db.collection("restaurants").document(user.getRestaurantId()).get()
                                    .addOnSuccessListener(restaurantDoc -> {
                                        if (restaurantDoc.exists()) {
                                            Restaurant restaurant = restaurantDoc.toObject(Restaurant.class);
                                            if (restaurant != null) {
                                                restaurantText.setText(restaurant.getName());
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle error loading restaurant
                                    });
                        }
                    }
                }
            }).addOnFailureListener(e -> {
                // Handle error loading user
            });
        }
    }

    private void setupListeners() {
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }
}