package com.example.expirytrack.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.adapter.IngredientAdapter;
import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.model.UsageHistory;
import com.example.expirytrack.util.ConnectivityHelper;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IngredientListFragment extends Fragment {
    private RecyclerView recyclerView;
    private IngredientAdapter adapter;
    private List<Ingredient> ingredients = new ArrayList<>();
    private List<Ingredient> filteredIngredients = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String restaurantId;
    private String currentSortOrder = "expiry"; // expiry, added, name
    private String currentCategory = "all";

    // UI Elements
    private SearchView searchView;
    private ChipGroup categoryChipGroup;
    private Spinner sortSpinner;
    private LinearLayout offlineBanner;
    private LinearLayout emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ingredient_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        recyclerView = view.findViewById(R.id.recyclerViewIngredients);
        searchView = view.findViewById(R.id.searchView);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        sortSpinner = view.findViewById(R.id.sortSpinner);
        offlineBanner = view.findViewById(R.id.offline_banner);
        emptyState = view.findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new IngredientAdapter(filteredIngredients, requireContext(), this::onIngredientAction);
        recyclerView.setAdapter(adapter);

        // Setup swipe gestures
        setupSwipeGestures();

        // Setup search view
        setupSearchView();

        // Setup category chips
        setupCategoryChips();

        // Setup sort spinner
        setupSortSpinner();

        // Check offline status
        checkOfflineStatus();

        fetchUserAndLoadIngredients();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterIngredients(newText);
                return true;
            }
        });
    }

    private void setupCategoryChips() {
        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View chip = group.findViewById(checkedId);
            if (chip != null) {
                currentCategory = chip.getContentDescription().toString();
                filterIngredients(searchView.getQuery().toString());
            }
        });
    }

    private void setupSortSpinner() {
        String[] sortOptions = { "⏱️ ใกล้หมดอายุก่อน", "📅 เพิ่มล่าสุด", "🔤 ชื่อ A-Z" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentSortOrder = "expiry";
                        break;
                    case 1:
                        currentSortOrder = "added";
                        break;
                    case 2:
                        currentSortOrder = "name";
                        break;
                }
                sortAndFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Ingredient ingredient = filteredIngredients.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe left = Mark as Used
                    markAsUsed(ingredient);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe right = Delete
                    confirmDelete(ingredient);
                }

                adapter.notifyDataSetChanged();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void checkOfflineStatus() {
        boolean isOnline = ConnectivityHelper.isNetworkAvailable(requireContext());
        offlineBanner.setVisibility(isOnline ? View.GONE : View.VISIBLE);
    }

    private void filterIngredients(String query) {
        filteredIngredients.clear();

        for (Ingredient ing : ingredients) {
            boolean matchesCategory = currentCategory.equals("all") ||
                    ing.getCategory().equalsIgnoreCase(currentCategory);
            boolean matchesQuery = ing.getName().toLowerCase()
                    .contains(query.toLowerCase());

            if (matchesCategory && matchesQuery) {
                filteredIngredients.add(ing);
            }
        }

        sortAndFilter();
    }

    private void sortAndFilter() {
        if (currentSortOrder.equals("expiry")) {
            filteredIngredients.sort((a, b) -> Long.compare(a.getExpiryDate(), b.getExpiryDate()));
        } else if (currentSortOrder.equals("added")) {
            filteredIngredients.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        } else if (currentSortOrder.equals("name")) {
            filteredIngredients.sort((a, b) -> a.getName().compareTo(b.getName()));
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredIngredients.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchUserAndLoadIngredients() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) {
            return;
        }

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                com.example.expirytrack.model.User user = documentSnapshot
                        .toObject(com.example.expirytrack.model.User.class);
                if (user != null && user.getRestaurantId() != null) {
                    restaurantId = user.getRestaurantId();
                    loadIngredients();
                }
            }
        }).addOnFailureListener(e -> {
            // Handle error
        });
    }

    private void loadIngredients() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            return;
        }

        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("IngredientListFragment", "Error loading ingredients", error);
                        return;
                    }
                    if (snapshot != null) {
                        ingredients.clear();
                        ingredients.addAll(snapshot.toObjects(Ingredient.class));
                        filterIngredients(searchView.getQuery().toString());
                    }
                });
    }

    private void onIngredientAction(Ingredient ingredient, String action) {
        if (action.equals("used")) {
            markAsUsed(ingredient);
        } else if (action.equals("edit")) {
            openEditDialog(ingredient);
        } else if (action.equals("delete")) {
            confirmDelete(ingredient);
        }
    }

    private void markAsUsed(Ingredient ingredient) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        ingredient.setStatus("used");
        ingredient.setUpdatedAt(System.currentTimeMillis());
        ingredient.setUpdatedBy(userId);
        db.collection("ingredients").document(ingredient.getId()).set(ingredient);
        recordUsageHistory(ingredient, "used");
        Toast.makeText(requireContext(), "✅ " + ingredient.getName() + " ใช้แล้ว", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(Ingredient ingredient) {
        new AlertDialog.Builder(requireContext())
                .setTitle("ยืนยันการลบ")
                .setMessage("ต้องการลบ \"" + ingredient.getName() + "\" หรือไม่?")
                .setPositiveButton("ลบ", (dialog, which) -> {
                    String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
                    ingredient.setStatus("deleted");
                    ingredient.setUpdatedAt(System.currentTimeMillis());
                    ingredient.setUpdatedBy(userId);
                    db.collection("ingredients").document(ingredient.getId()).set(ingredient);
                    recordUsageHistory(ingredient, "deleted");
                    Toast.makeText(requireContext(), "🗑️ " + ingredient.getName() + " ลบแล้ว", Toast.LENGTH_SHORT)
                            .show();
                })
                .setNegativeButton("ยกเลิก", null)
                .show();
    }

    private void openEditDialog(Ingredient ingredient) {
        EditIngredientDialog dialog = EditIngredientDialog.newInstance(ingredient);
        dialog.show(getChildFragmentManager(), "EditIngredient");
    }

    private void recordUsageHistory(Ingredient ingredient, String action) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        String userName = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Unknown";

        UsageHistory history = new UsageHistory(
                ingredient.getId(),
                ingredient.getRestaurantId(),
                ingredient.getName(),
                action,
                userId,
                userName,
                new java.util.Date());
        db.collection("usageHistory").add(history);
    }
}
