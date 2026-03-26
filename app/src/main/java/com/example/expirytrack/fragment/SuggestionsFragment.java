package com.example.expirytrack.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.adapter.SuggestionsAdapter;
import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.model.IngredientSuggestionGroup;
import com.example.expirytrack.model.MenuSuggestion;
import com.example.expirytrack.util.GeminiService;
import com.example.expirytrack.util.MenuCacheManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SuggestionsFragment extends Fragment {

    private static final int DEFAULT_NOTIFY_DAYS = 3; // fallback if field missing

    private RecyclerView recyclerSuggestions;
    private LinearLayout emptyState;
    private LinearLayout loadingState;
    private MaterialButton btnRefreshSuggestions;

    private SuggestionsAdapter adapter;
    private final List<IngredientSuggestionGroup> groups = new ArrayList<>();
    private final List<String> allActiveIngredients = new ArrayList<>();

    private GeminiService geminiService;
    private FirebaseFirestore db;
    private MenuCacheManager cacheManager;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggestions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerSuggestions = view.findViewById(R.id.recyclerSuggestions);
        emptyState = view.findViewById(R.id.emptyState);
        loadingState = view.findViewById(R.id.loadingState);
        btnRefreshSuggestions = view.findViewById(R.id.btnRefreshSuggestions);

        db = FirebaseFirestore.getInstance();
        geminiService = new GeminiService();
        cacheManager = new MenuCacheManager(requireContext());

        // Set up RecyclerView with adapter
        adapter = new SuggestionsAdapter(
                requireContext(),
                groups,
                allActiveIngredients,
                geminiService,
                this::onMenuSelected);
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSuggestions.setAdapter(adapter);

        // Refresh button: clear cache and reload
        btnRefreshSuggestions.setOnClickListener(v -> {
            cacheManager.clearAllMenuCache();
            // Clear group data and reload
            for (IngredientSuggestionGroup group : groups) {
                group.setExpanded(false);
                group.setMenus(null);
                group.setHasLoaded(false);
                group.setLoading(false);
            }
            adapter.notifyDataSetChanged();
        });

        showLoading(true);

        // Step 1: resolve restaurantId from current user's Firestore record
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showLoading(false);
            showEmpty(true);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded())
                        return;
                    restaurantId = userDoc.getString("restaurantId");
                    if (restaurantId == null || restaurantId.isEmpty()) {
                        showLoading(false);
                        showEmpty(true);
                        return;
                    }
                    loadIngredients();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded())
                        return;
                    showLoading(false);
                    showEmpty(true);
                });
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadIngredients() {
        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded())
                        return;

                    long nowMs = System.currentTimeMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    List<IngredientSuggestionGroup> nearExpiry = new ArrayList<>();
                    allActiveIngredients.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Ingredient ing = doc.toObject(Ingredient.class);
                        if (ing == null)
                            continue;
                        if (ing.getName() == null || ing.getName().isEmpty())
                            continue;

                        allActiveIngredients.add(ing.getName());

                        long expiryMs = ing.getExpiryDate();
                        long diffMs = expiryMs - nowMs;
                        int daysLeft = (int) TimeUnit.MILLISECONDS.toDays(diffMs);

                        int threshold = ing.getNotifyDaysBefore() > 0
                                ? ing.getNotifyDaysBefore()
                                : DEFAULT_NOTIFY_DAYS;

                        if (daysLeft >= 0 && daysLeft <= threshold) {
                            String formatted = sdf.format(new Date(expiryMs));
                            nearExpiry.add(new IngredientSuggestionGroup(
                                    ing.getName(), daysLeft, formatted));
                        }
                    }

                    // Sort near-expiry ASC by daysLeft (most urgent first)
                    Collections.sort(nearExpiry,
                            (a, b) -> Integer.compare(a.getDaysLeft(), b.getDaysLeft()));

                    groups.clear();
                    groups.addAll(nearExpiry);
                    adapter.notifyDataSetChanged();

                    showLoading(false);
                    showEmpty(groups.isEmpty());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded())
                        return;
                    showLoading(false);
                    showEmpty(true);
                });
    }

    // -------------------------------------------------------------------------
    // Menu item click → open bottom sheet
    // -------------------------------------------------------------------------

    private void onMenuSelected(MenuSuggestion menu, String ingredientName, int daysLeft) {
        MenuDetailBottomSheet sheet = MenuDetailBottomSheet.newInstance(menu, ingredientName, daysLeft);
        sheet.show(getParentFragmentManager(), "MenuDetail");
    }

    // -------------------------------------------------------------------------
    // View state helpers
    // -------------------------------------------------------------------------

    private void showLoading(boolean show) {
        loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerSuggestions.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerSuggestions.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}