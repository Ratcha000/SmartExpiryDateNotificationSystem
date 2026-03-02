package com.example.expirytrack.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.adapter.TeamMembersAdapter;
import com.example.expirytrack.adapter.UsageHistoryAdapter;
import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.model.User;
import com.example.expirytrack.model.UsageHistory;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Fragment for Managers
 * Displays: Overview cards, Usage history, Category summary, Team management
 */
public class DashboardFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String restaurantId;

    // Overview Cards
    private TextView totalIngredientsText;
    private TextView expiringCountText;
    private TextView expiredCountText;
    private TextView usedThisMonthText;

    // Usage History
    private RecyclerView usageHistoryRecyclerView;
    private UsageHistoryAdapter usageHistoryAdapter;
    private List<UsageHistory> usageHistoryList = new ArrayList<>();
    private Spinner filterSpinner;

    // Team Management
    private TextView inviteCodeText;
    private Button btnCopyCode;
    private Button btnRegenerateCode;
    private RecyclerView teamMembersRecyclerView;
    private TeamMembersAdapter teamMembersAdapter;
    private List<User> teamMembersList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews(view);
        fetchUserRestaurantId();
    }

    private void initializeViews(View view) {
        // Overview Cards
        totalIngredientsText = view.findViewById(R.id.totalIngredientsText);
        expiringCountText = view.findViewById(R.id.expiringCountText);
        expiredCountText = view.findViewById(R.id.expiredCountText);
        usedThisMonthText = view.findViewById(R.id.usedThisMonthText);

        // Usage History
        usageHistoryRecyclerView = view.findViewById(R.id.usageHistoryRecyclerView);
        usageHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        usageHistoryAdapter = new UsageHistoryAdapter(usageHistoryList);
        usageHistoryRecyclerView.setAdapter(usageHistoryAdapter);

        filterSpinner = view.findViewById(R.id.filterSpinner);
        setupFilterSpinner();

        // Team Management
        inviteCodeText = view.findViewById(R.id.inviteCodeText);
        btnCopyCode = view.findViewById(R.id.btnCopyCode);
        btnRegenerateCode = view.findViewById(R.id.btnRegenerateCode);
        teamMembersRecyclerView = view.findViewById(R.id.teamMembersRecyclerView);
        teamMembersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        teamMembersAdapter = new TeamMembersAdapter(teamMembersList);
        teamMembersRecyclerView.setAdapter(teamMembersAdapter);

        btnCopyCode.setOnClickListener(v -> copyInviteCode());
        btnRegenerateCode.setOnClickListener(v -> regenerateInviteCode());
    }

    private void fetchUserRestaurantId() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) {
            return;
        }

        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                restaurantId = doc.getString("restaurantId");
                loadDashboardData();
                loadInviteCode();
            }
        });
    }

    private void loadDashboardData() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            return;
        }

        // Load overview cards
        loadOverviewCards();

        // Load usage history
        loadUsageHistory("all");

        // Load team members
        loadTeamMembers();
    }

    private void loadOverviewCards() {
        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        List<Ingredient> activeIngredients = snapshot.toObjects(Ingredient.class);
                        int total = activeIngredients.size();
                        int expiring = 0;

                        Calendar today = Calendar.getInstance();
                        for (Ingredient ing : activeIngredients) {
                            long daysLeft = calculateDaysUntilExpiry(ing.getExpiryDate());
                            if (daysLeft <= 3) {
                                expiring++;
                            }
                        }

                        totalIngredientsText.setText(String.valueOf(total));
                        expiringCountText.setText(String.valueOf(expiring));

                        // Load expired count
                        loadExpiredCount();

                        // Load used this month count
                        loadUsedThisMonth();
                    }
                });
    }

    private void loadExpiredCount() {
        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "expired")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        expiredCountText.setText(String.valueOf(snapshot.size()));
                    }
                });
    }

    private void loadUsedThisMonth() {
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);

        java.util.Date startDate = startOfMonth.getTime();

        db.collection("usageHistory")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("action", "used")
                .whereGreaterThanOrEqualTo("date", startDate)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        usedThisMonthText.setText(String.valueOf(snapshot.size()));
                    }
                });
    }

    private void setupFilterSpinner() {
        String[] filterOptions = { "ทั้งหมด", "เดือนนี้", "7 วันล่าสุด" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = filterOptions[position];
                if (selected.equals("ทั้งหมด")) {
                    loadUsageHistory("all");
                } else if (selected.equals("เดือนนี้")) {
                    loadUsageHistory("month");
                } else {
                    loadUsageHistory("week");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUsageHistory(String filter) {
        if (restaurantId == null || restaurantId.isEmpty()) {
            return;
        }

        Query query = db.collection("usageHistory")
                .whereEqualTo("restaurantId", restaurantId)
                .orderBy("date", Query.Direction.DESCENDING);

        // Apply time filter
        if (!filter.equals("all")) {
            Calendar calendar = Calendar.getInstance();
            if (filter.equals("month")) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else if (filter.equals("week")) {
                calendar.add(Calendar.DAY_OF_YEAR, -7);
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            java.util.Date filterDate = calendar.getTime();
            query = query.whereGreaterThanOrEqualTo("date", filterDate);
        }

        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot != null) {
                usageHistoryList.clear();
                usageHistoryList.addAll(snapshot.toObjects(UsageHistory.class));
                usageHistoryAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadInviteCode() {
        db.collection("restaurants").document(restaurantId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String code = doc.getString("inviteCode");
                        inviteCodeText.setText(code);
                    }
                });
    }

    private void copyInviteCode() {
        String code = inviteCodeText.getText().toString();
        if (!code.isEmpty()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext()
                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Invite Code", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied: " + code, Toast.LENGTH_SHORT).show();
        }
    }

    private void regenerateInviteCode() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            return;
        }

        String newCode = generateRandomCode();
        db.collection("restaurants").document(restaurantId)
                .update("inviteCode", newCode)
                .addOnSuccessListener(aVoid -> {
                    inviteCodeText.setText(newCode);
                    Toast.makeText(requireContext(), "Code regenerated: " + newCode, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTeamMembers() {
        db.collection("users")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        teamMembersList.clear();
                        List<User> users = snapshot.toObjects(User.class);
                        teamMembersList.addAll(users);
                        teamMembersAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load team members", Toast.LENGTH_SHORT).show();
                });
    }

    private String generateRandomCode() {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                .substring((int) (Math.random() * 10), (int) (Math.random() * 10) + 6);
    }

    private long calculateDaysUntilExpiry(long expiryDate) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar expiry = Calendar.getInstance();
        expiry.setTimeInMillis(expiryDate);
        expiry.set(Calendar.HOUR_OF_DAY, 0);
        expiry.set(Calendar.MINUTE, 0);
        expiry.set(Calendar.SECOND, 0);

        return (expiry.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);
    }
}
