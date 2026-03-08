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
import android.widget.TextView;
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
import com.example.expirytrack.repository.FirestoreRepository;
import com.example.expirytrack.util.ConnectivityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HomeFragment — primary ingredient list screen for Employee.
 * Merged from the former IngredientListFragment; includes:
 *   - Real-time Firestore snapshot listener
 *   - Search, category filter chips, sort spinner
 *   - Quick-stats bar (safe / warning / expired counts)
 *   - Swipe-to-action (left = used, right = delete)
 *   - Working EditIngredientDialog
 *   - Offline banner
 *   - Empty state
 */
public class HomeFragment extends Fragment implements IngredientAdapter.OnIngredientActionListener {

    private RecyclerView recyclerView;
    private IngredientAdapter adapter;
    private List<Ingredient> allIngredients = new ArrayList<>();
    private List<Ingredient> filteredIngredients = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirestoreRepository repo;
    private String restaurantId;

    // UI
    private SearchView searchView;
    private ChipGroup categoryChipGroup;
    private Spinner sortSpinner;
    private TextView safeCountText, warningCountText, expiredCountText;
    private View emptyState;
    private LinearLayout offlineBanner;
    private MaterialButton addButton;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        repo = new FirestoreRepository();

        initViews(view);
        setupRecyclerView();
        setupSearchView();
        setupCategoryFilter();
        setupSortSpinner();
        setupButtons();
        setupSwipeGestures();
        checkOfflineStatus();
        fetchUserAndLoadIngredients();

        return view;
    }

    // ─────────────────────────────────── INIT ───────────────────────────────────

    private void initViews(View view) {
        recyclerView      = view.findViewById(R.id.recyclerViewIngredients);
        searchView        = view.findViewById(R.id.searchView);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        sortSpinner       = view.findViewById(R.id.sortSpinner);
        safeCountText     = view.findViewById(R.id.safeCountText);
        warningCountText  = view.findViewById(R.id.warningCountText);
        expiredCountText  = view.findViewById(R.id.expiredCountText);
        emptyState        = view.findViewById(R.id.emptyState);
        offlineBanner     = view.findViewById(R.id.offline_banner);
        addButton         = view.findViewById(R.id.addButton);
        fab               = view.findViewById(R.id.fab);
    }

    private void setupRecyclerView() {
        adapter = new IngredientAdapter(filteredIngredients, getContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ─────────────────────────────────── SEARCH / FILTER / SORT ────────────────

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }

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
        String[] sortOptions = {
                "วันหมดอายุ (ใกล้สุดก่อน)",
                "ชื่อ A-Z",
                "ชื่อ Z-A",
                "วันที่เพิ่ม (ใหม่ก่อน)"
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortIngredients(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ─────────────────────────────────── BUTTONS ────────────────────────────────

    private void setupButtons() {
        if (addButton != null) addButton.setOnClickListener(v -> showAddIngredientDialog());
        if (fab != null)       fab.setOnClickListener(v -> showAddIngredientDialog());
    }

    private void showAddIngredientDialog() {
        if (restaurantId != null && !restaurantId.isEmpty()) {
            long defaultDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
            AddIngredientDialog dialog = AddIngredientDialog.newInstance(defaultDate, restaurantId);
            dialog.show(getChildFragmentManager(), "AddIngredient");
        } else {
            Toast.makeText(getContext(), "ไม่สามารถระบุร้านอาหารได้ กรุณาลองใหม่", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────── SWIPE GESTURES ─────────────────────────

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= filteredIngredients.size()) return;
                Ingredient ingredient = filteredIngredients.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe left = Mark as used
                    markAsUsed(ingredient);
                } else {
                    // Swipe right = Delete
                    // Restore item first, then show confirm dialog
                    adapter.notifyItemChanged(position);
                    confirmDelete(ingredient);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    // ─────────────────────────────────── OFFLINE BANNER ─────────────────────────

    private void checkOfflineStatus() {
        if (offlineBanner == null) return;
        boolean online = ConnectivityHelper.isNetworkAvailable(requireContext());
        offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    // ─────────────────────────────────── DATA LOADING ────────────────────────────

    private void fetchUserAndLoadIngredients() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        com.example.expirytrack.model.User user =
                                doc.toObject(com.example.expirytrack.model.User.class);
                        if (user != null && user.getRestaurantId() != null) {
                            restaurantId = user.getRestaurantId();
                            loadIngredients();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "โหลดข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show());
    }

    private void loadIngredients() {
        if (restaurantId == null || restaurantId.isEmpty()) return;

        db.collection("ingredients")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null) {
                        allIngredients.clear();
                        allIngredients.addAll(snapshot.toObjects(Ingredient.class));
                        updateStats();
                        filterIngredients();
                    }
                });
    }

    // ─────────────────────────────────── STATS ───────────────────────────────────

    private void updateStats() {
        int safeCount = 0, warningCount = 0, expiredCount = 0;
        long now = System.currentTimeMillis();
        for (Ingredient ing : allIngredients) {
            long daysLeft = (ing.getExpiryDate() - now) / (1000 * 60 * 60 * 24);
            if (daysLeft < 0)                          expiredCount++;
            else if (daysLeft <= ing.getNotifyDaysBefore()) warningCount++;
            else                                        safeCount++;
        }
        if (safeCountText    != null) safeCountText.setText(String.valueOf(safeCount));
        if (warningCountText != null) warningCountText.setText(String.valueOf(warningCount));
        if (expiredCountText != null) expiredCountText.setText(String.valueOf(expiredCount));
    }

    // ─────────────────────────────────── FILTER / SORT ───────────────────────────

    private void filterIngredients() {
        filteredIngredients.clear();
        String query = searchView != null ? searchView.getQuery().toString().toLowerCase() : "";
        int chipId = categoryChipGroup != null ? categoryChipGroup.getCheckedChipId() : View.NO_ID;
        String selectedCategory = getSelectedCategory(chipId);

        for (Ingredient ing : allIngredients) {
            boolean matchSearch = ing.getName().toLowerCase().contains(query);
            boolean matchCat = selectedCategory.equals("ทั้งหมด")
                    || ing.getCategory().equals(selectedCategory);
            if (matchSearch && matchCat) filteredIngredients.add(ing);
        }

        sortIngredients(sortSpinner != null ? sortSpinner.getSelectedItemPosition() : 0);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private String getSelectedCategory(int chipId) {
        if (chipId == View.NO_ID || chipId == R.id.chip_all) return "ทั้งหมด";
        if (chipId == R.id.chip_meat)       return "เนื้อสัตว์";
        if (chipId == R.id.chip_vegetables) return "ผักและผลไม้";
        if (chipId == R.id.chip_dairy)      return "นมและไข่";
        if (chipId == R.id.chip_seasoning)  return "เครื่องปรุง";
        if (chipId == R.id.chip_dry_goods)  return "ของแห้ง";
        if (chipId == R.id.chip_beverages)  return "เครื่องดื่ม";
        if (chipId == R.id.chip_others)     return "อื่นๆ";
        return "ทั้งหมด";
    }

    private void sortIngredients(int option) {
        switch (option) {
            case 0: Collections.sort(filteredIngredients, Comparator.comparingLong(Ingredient::getExpiryDate)); break;
            case 1: Collections.sort(filteredIngredients, (a, b) -> a.getName().compareToIgnoreCase(b.getName())); break;
            case 2: Collections.sort(filteredIngredients, (a, b) -> b.getName().compareToIgnoreCase(a.getName())); break;
            case 3: Collections.sort(filteredIngredients, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt())); break;
        }
        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        if (emptyState == null) return;
        emptyState.setVisibility(filteredIngredients.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────── ACTIONS ─────────────────────────────────

    /** IngredientAdapter callback */
    @Override
    public void onAction(Ingredient ingredient, String action) {
        switch (action) {
            case "used":   markAsUsed(ingredient);    break;
            case "delete": confirmDelete(ingredient); break;
            case "edit":   openEditDialog(ingredient); break;
        }
    }

    private void markAsUsed(Ingredient ingredient) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        repo.markIngredientUsed(ingredient.getId(), userId, (success, error) -> {
            if (success) {
                recordUsageHistory(ingredient, "used");
                Toast.makeText(getContext(), "✅ " + ingredient.getName() + " ใช้แล้ว", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "เกิดข้อผิดพลาด: " + error, Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged(); // restore swipe
            }
        });
    }

    private void confirmDelete(Ingredient ingredient) {
        new AlertDialog.Builder(requireContext())
                .setTitle("ยืนยันการลบ")
                .setMessage("ต้องการลบ \"" + ingredient.getName() + "\" หรือไม่?")
                .setPositiveButton("ลบ", (dialog, which) -> {
                    String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
                    repo.deleteIngredient(ingredient.getId(), userId, (success, error) -> {
                        if (success) {
                            recordUsageHistory(ingredient, "deleted");
                            Toast.makeText(getContext(),
                                    "🗑️ " + ingredient.getName() + " ลบแล้ว", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "เกิดข้อผิดพลาด: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ยกเลิก", null)
                .show();
    }

    private void openEditDialog(Ingredient ingredient) {
        EditIngredientDialog dialog = EditIngredientDialog.newInstance(ingredient);
        dialog.show(getChildFragmentManager(), "EditIngredient");
    }

    private void recordUsageHistory(Ingredient ingredient, String action) {
        String userId   = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        String userName = auth.getCurrentUser() != null
                ? (auth.getCurrentUser().getDisplayName() != null
                        ? auth.getCurrentUser().getDisplayName() : "Unknown")
                : "Unknown";

        UsageHistory history = new UsageHistory(
                ingredient.getId(),
                ingredient.getRestaurantId(),
                ingredient.getName(),
                action,
                userId,
                userName,
                new java.util.Date());
        repo.addUsageHistory(history, (success, error) -> {
            if (!success) android.util.Log.e("HomeFragment", "Failed to record history: " + error);
        });
    }
}