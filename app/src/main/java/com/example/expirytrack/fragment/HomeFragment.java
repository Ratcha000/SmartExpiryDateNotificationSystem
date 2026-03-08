package com.example.expirytrack.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.adapter.IngredientAdapter;
import com.example.expirytrack.model.Ingredient;
import com.example.expirytrack.model.UsageHistory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment implements IngredientAdapter.OnIngredientActionListener {

    private RecyclerView recyclerView;
    private IngredientAdapter adapter;
    private List<Ingredient> allIngredients = new ArrayList<>();
    private List<Ingredient> filteredIngredients = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String restaurantId;

    private SearchView searchView;
    private ChipGroup categoryChipGroup;
    private Spinner sortSpinner;
    private TextView safeCountText, warningCountText, expiredCountText;
    private View emptyState;
    private MaterialButton addButton;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        setupSearchView();
        setupCategoryFilter();
        setupSortSpinner();
        setupButtons();

        fetchUserAndLoadIngredients();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewIngredients);
        searchView = view.findViewById(R.id.searchView);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        sortSpinner = view.findViewById(R.id.sortSpinner);
        safeCountText = view.findViewById(R.id.safeCountText);
        warningCountText = view.findViewById(R.id.warningCountText);
        expiredCountText = view.findViewById(R.id.expiredCountText);
        emptyState = view.findViewById(R.id.emptyState);
        addButton = view.findViewById(R.id.addButton);
        fab = view.findViewById(R.id.fab);
    }

    private void setupRecyclerView() {
        adapter = new IngredientAdapter(filteredIngredients, getContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterIngredients();
                return true;
            }
        });
    }

    private void setupCategoryFilter() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> filterIngredients());
    }

    private void setupSortSpinner() {
        String[] sortOptions = { "วันหมดอายุ (ใกล้สุดก่อน)", "ชื่อ A-Z", "ชื่อ Z-A", "วันที่เพิ่ม (ใหม่ก่อน)" };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortIngredients(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupButtons() {
        addButton.setOnClickListener(v -> showAddIngredientDialog());
        fab.setOnClickListener(v -> showAddIngredientDialog());
    }

    private void showAddIngredientDialog() {
        if (restaurantId != null && !restaurantId.isEmpty()) {
            long defaultDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // 7 วันข้างหน้า
            AddIngredientDialog dialog = AddIngredientDialog.newInstance(defaultDate, restaurantId);
            dialog.show(getChildFragmentManager(), "AddIngredient");
        } else {
            Toast.makeText(getContext(), "ไม่สามารถระบุร้านอาหารได้ กรุณาลองใหม่", Toast.LENGTH_SHORT).show();
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
                        return;
                    }
                    if (snapshot != null) {
                        allIngredients.clear();
                        allIngredients.addAll(snapshot.toObjects(Ingredient.class));
                        updateStats();
                        filterIngredients();
                    }
                });
    }

    private void updateStats() {
        int safeCount = 0, warningCount = 0, expiredCount = 0;
        long currentTime = System.currentTimeMillis();

        for (Ingredient ingredient : allIngredients) {
            long daysLeft = (ingredient.getExpiryDate() - currentTime) / (1000 * 60 * 60 * 24);

            if (daysLeft < 0) {
                expiredCount++;
            } else if (daysLeft <= ingredient.getNotifyDaysBefore()) {
                warningCount++;
            } else {
                safeCount++;
            }
        }

        safeCountText.setText(String.valueOf(safeCount));
        warningCountText.setText(String.valueOf(warningCount));
        expiredCountText.setText(String.valueOf(expiredCount));
    }

    private void filterIngredients() {
        filteredIngredients.clear();

        String query = searchView.getQuery().toString().toLowerCase();
        int selectedChipId = categoryChipGroup.getCheckedChipId();
        String selectedCategory = getSelectedCategory(selectedChipId);

        for (Ingredient ingredient : allIngredients) {
            boolean matchesSearch = ingredient.getName().toLowerCase().contains(query);
            boolean matchesCategory = selectedCategory.equals("ทั้งหมด") ||
                    ingredient.getCategory().equals(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredIngredients.add(ingredient);
            }
        }

        sortIngredients(sortSpinner.getSelectedItemPosition());
        adapter.notifyDataSetChanged();

        emptyState.setVisibility(filteredIngredients.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getSelectedCategory(int selectedChipId) {
        if (selectedChipId == View.NO_ID || selectedChipId == R.id.chip_all)
            return "ทั้งหมด";

        if (selectedChipId == R.id.chip_meat) {
            return "เนื้อสัตว์";
        } else if (selectedChipId == R.id.chip_vegetables) {
            return "ผักและผลไม้";
        } else if (selectedChipId == R.id.chip_dairy) {
            return "นมและไข่";
        } else if (selectedChipId == R.id.chip_seasoning) {
            return "เครื่องปรุง";
        } else if (selectedChipId == R.id.chip_dry_goods) {
            return "ของแห้ง";
        } else if (selectedChipId == R.id.chip_beverages) {
            return "เครื่องดื่ม";
        } else if (selectedChipId == R.id.chip_others) {
            return "อื่นๆ";
        }
        return "ทั้งหมด";
    }

    private void sortIngredients(int sortOption) {
        switch (sortOption) {
            case 0: // วันหมดอายุ (ใกล้สุดก่อน)
                Collections.sort(filteredIngredients, Comparator.comparingLong(Ingredient::getExpiryDate));
                break;
            case 1: // ชื่อ A-Z
                Collections.sort(filteredIngredients, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 2: // ชื่อ Z-A
                Collections.sort(filteredIngredients, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case 3: // วันที่เพิ่ม (ใหม่ก่อน)
                Collections.sort(filteredIngredients, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAction(Ingredient ingredient, String action) {
        if ("used".equals(action)) {
            markAsUsed(ingredient);
        } else if ("delete".equals(action)) {
            confirmDelete(ingredient);
        } else if ("edit".equals(action)) {
            openEditDialog(ingredient);
        }
    }

    private void markAsUsed(Ingredient ingredient) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        ingredient.setStatus("used");
        ingredient.setUpdatedAt(System.currentTimeMillis());
        ingredient.setUpdatedBy(userId);
        db.collection("ingredients").document(ingredient.getId()).set(ingredient)
                .addOnSuccessListener(aVoid -> {
                    recordUsageHistory(ingredient, "used");
                    Toast.makeText(getContext(), "✅ " + ingredient.getName() + " ใช้แล้ว", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "เกิดข้อผิดพลาด: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(Ingredient ingredient) {
        new AlertDialog.Builder(getContext())
                .setTitle("ยืนยันการลบ")
                .setMessage("ต้องการลบ \"" + ingredient.getName() + "\" หรือไม่?")
                .setPositiveButton("ลบ", (dialog, which) -> {
                    String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
                    ingredient.setStatus("deleted");
                    ingredient.setUpdatedAt(System.currentTimeMillis());
                    ingredient.setUpdatedBy(userId);
                    db.collection("ingredients").document(ingredient.getId()).set(ingredient)
                            .addOnSuccessListener(aVoid -> {
                                recordUsageHistory(ingredient, "deleted");
                                Toast.makeText(getContext(), "🗑️ " + ingredient.getName() + " ลบแล้ว",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast
                                    .makeText(getContext(), "เกิดข้อผิดพลาด: " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show());
                })
                .setNegativeButton("ยกเลิก", null)
                .show();
    }

    private void openEditDialog(Ingredient ingredient) {
        Toast.makeText(getContext(), "ฟีเจอร์แก้ไขกำลังพัฒนา", Toast.LENGTH_SHORT).show();
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