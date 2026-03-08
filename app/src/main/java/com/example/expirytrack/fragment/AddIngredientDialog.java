package com.example.expirytrack.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.expirytrack.R;
import com.example.expirytrack.model.Ingredient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

/**
 * Dialog for adding new ingredients (used after scanning)
 */
public class AddIngredientDialog extends DialogFragment {
    private static final String ARG_EXPIRY_DATE = "expiryDate";
    private static final String ARG_RESTAURANT_ID = "restaurantId";

    private EditText editName;
    private Spinner spinnerCategory;
    private Button btnEditDate;
    private Button btnSave;
    private Button btnCancel;
    private TextView scannedDateDisplay;
    private NumberPicker numberPickerDays;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private long expiryDate;
    private String restaurantId;
    private long selectedDate;

    public static AddIngredientDialog newInstance(long expiryDate, String restaurantId) {
        AddIngredientDialog dialog = new AddIngredientDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_EXPIRY_DATE, expiryDate);
        args.putString(ARG_RESTAURANT_ID, restaurantId);
        dialog.setArguments(args);
        return dialog;
    }

    // Constructor สำหรับเรียกจากภายนอกโดยไม่มี parameters
    public static AddIngredientDialog newInstance() {
        long defaultDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // 7 วันข้างหน้า
        return newInstance(defaultDate, "");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            expiryDate = getArguments().getLong(ARG_EXPIRY_DATE, System.currentTimeMillis());
            restaurantId = getArguments().getString(ARG_RESTAURANT_ID, "");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        selectedDate = expiryDate;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_ingredient, null);

        editName = view.findViewById(R.id.editIngredientName);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnEditDate = view.findViewById(R.id.btnEditDate);
        scannedDateDisplay = view.findViewById(R.id.scannedDateDisplay);
        numberPickerDays = view.findViewById(R.id.numberPickerNotifyDays);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        setupSpinner();
        setupNumberPicker();
        updateDateDisplay();

        btnEditDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveIngredient());
        btnCancel.setOnClickListener(v -> dismiss());

        AlertDialog dialog = builder.setView(view)
                .setTitle("เพิ่มวัตถุดิบ")
                .create();

        // Set custom button behavior
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "บันทึก", (d, w) -> saveIngredient());
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "ยกเลิก", (d, w) -> dismiss());

        return dialog;
    }

    private void setupSpinner() {
        String[] categories = { "เนื้อสัตว์", "ผักและผลไม้", "นมและไข่", "เครื่องปรุง", "ของแห้ง", "เครื่องดื่ม",
                "อื่นๆ" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(0);
    }

    private void setupNumberPicker() {
        numberPickerDays.setMinValue(1);
        numberPickerDays.setMaxValue(14);
        numberPickerDays.setValue(3); // Default 3 days
    }

    private void updateDateDisplay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        String dateStr = String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        scannedDateDisplay.setText(dateStr);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate = selected.getTimeInMillis();
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void saveIngredient() {
        String name = editName.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editName.setError("กรุณากรอกชื่อวัตถุดิบ");
            return;
        }

        if (TextUtils.isEmpty(restaurantId)) {
            android.widget.Toast.makeText(requireContext(),
                    "ข้อมูลร้านอาหารไม่ถูกต้อง", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        String category = (String) spinnerCategory.getSelectedItem();
        int notifyDays = numberPickerDays.getValue();

        // สร้างข้อมูลวัตถุดิบ
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setCategory(category);
        ingredient.setExpiryDate(selectedDate);
        ingredient.setNotifyDaysBefore(notifyDays);
        ingredient.setRestaurantId(restaurantId);
        ingredient.setStatus("active");
        ingredient.setCreatedAt(System.currentTimeMillis());
        ingredient.setUpdatedAt(System.currentTimeMillis());
        ingredient.setUpdatedBy(userId);
        ingredient.setScannedBy(userId);
        ingredient.setScannedAt(System.currentTimeMillis());

        // แสดง loading
        btnSave.setEnabled(false);
        btnSave.setText("กำลังบันทึก...");

        // บันทึกลง Firestore
        db.collection("ingredients").add(ingredient)
                .addOnSuccessListener(documentReference -> {
                    // อัปเดต ID ของวัตถุดิบ
                    ingredient.setId(documentReference.getId());
                    db.collection("ingredients").document(documentReference.getId()).set(ingredient)
                            .addOnSuccessListener(aVoid -> {
                                android.widget.Toast.makeText(requireContext(),
                                        "✅ บันทึก " + name + " เรียบร้อยแล้ว",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> showSaveError(e.getMessage()));
                })
                .addOnFailureListener(e -> showSaveError(e.getMessage()));
    }

    private void showSaveError(String errorMessage) {
        btnSave.setEnabled(true);
        btnSave.setText("บันทึก");
        android.widget.Toast.makeText(requireContext(),
                "❌ เกิดข้อผิดพลาดในการบันทึก: " + errorMessage,
                android.widget.Toast.LENGTH_LONG).show();
    }
}
