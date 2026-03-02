package com.example.expirytrack.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.expirytrack.R;
import com.example.expirytrack.model.Ingredient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class EditIngredientDialog extends DialogFragment {
    private static final String ARG_INGREDIENT = "ingredient";
    private Ingredient ingredient;
    private EditText editName;
    private Spinner spinnerCategory;
    private Button btnDate;
    private NumberPicker numberPickerDays;
    private FirebaseFirestore db;
    private long selectedDate;

    public static EditIngredientDialog newInstance(Ingredient ingredient) {
        EditIngredientDialog dialog = new EditIngredientDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_INGREDIENT, ingredient);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ingredient = (Ingredient) getArguments().getSerializable(ARG_INGREDIENT);
        }
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_ingredient, null);

        editName = view.findViewById(R.id.editIngredientName);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnDate = view.findViewById(R.id.btnSelectDate);
        numberPickerDays = view.findViewById(R.id.numberPickerNotifyDays);

        setupSpinner();
        setupNumberPicker();

        if (ingredient != null) {
            editName.setText(ingredient.getName());
            selectedDate = ingredient.getExpiryDate();
            updateDateButton();
            spinnerCategory.setSelection(getCategoryIndex(ingredient.getCategory()));
            numberPickerDays.setValue(ingredient.getNotifyDaysBefore());
        }

        btnDate.setOnClickListener(v -> showDatePicker());

        builder.setView(view)
                .setTitle("แก้ไขวัตถุดิบ")
                .setPositiveButton("บันทึก", (dialog, which) -> saveIngredient())
                .setNegativeButton("ยกเลิก", null);

        return builder.create();
    }

    private void setupSpinner() {
        String[] categories = { "เนื้อสัตว์", "ผักและผลไม้", "นมและไข่", "เครื่องปรุง", "ของแห้ง", "เครื่องดื่ม",
                "อื่นๆ" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupNumberPicker() {
        numberPickerDays.setMinValue(1);
        numberPickerDays.setMaxValue(14);
        numberPickerDays.setValue(ingredient != null ? ingredient.getNotifyDaysBefore() : 3);
    }

    private int getCategoryIndex(String category) {
        String[] categories = { "เนื้อสัตว์", "ผักและผลไม้", "นมและไข่", "เครื่องปรุง", "ของแห้ง", "เครื่องดื่ม",
                "อื่นๆ" };
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return i;
            }
        }
        return 0;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate > 0 ? selectedDate : System.currentTimeMillis());

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate = selected.getTimeInMillis();
                    updateDateButton();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void updateDateButton() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        String dateStr = String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
        btnDate.setText(dateStr);
    }

    private void saveIngredient() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            editName.setError("กรุณากรอกชื่อสินค้า");
            return;
        }

        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        ingredient.setName(name);
        ingredient.setCategory((String) spinnerCategory.getSelectedItem());
        ingredient.setExpiryDate(selectedDate);
        ingredient.setNotifyDaysBefore(numberPickerDays.getValue());
        ingredient.setUpdatedAt(System.currentTimeMillis());
        ingredient.setUpdatedBy(userId);

        db.collection("ingredients").document(ingredient.getId()).set(ingredient);
        dismiss();
    }
}
