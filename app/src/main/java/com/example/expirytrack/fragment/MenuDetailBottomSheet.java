package com.example.expirytrack.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.expirytrack.R;
import com.example.expirytrack.model.MenuSuggestion;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class MenuDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MENU_NAME           = "menu_name";
    private static final String ARG_INGREDIENTS         = "ingredients";
    private static final String ARG_INGREDIENTS_IN_STOCK = "ingredients_in_stock";
    private static final String ARG_STEPS               = "steps";
    private static final String ARG_INGREDIENT_NAME     = "ingredient_name";
    private static final String ARG_DAYS_LEFT           = "days_left";

    public static MenuDetailBottomSheet newInstance(MenuSuggestion menu,
                                                     String ingredientName,
                                                     int daysLeft) {
        MenuDetailBottomSheet sheet = new MenuDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MENU_NAME, menu.getMenuName());
        args.putStringArrayList(ARG_INGREDIENTS,
                menu.getIngredients() != null
                        ? new java.util.ArrayList<>(menu.getIngredients())
                        : new java.util.ArrayList<>());
        args.putStringArrayList(ARG_INGREDIENTS_IN_STOCK,
                menu.getIngredientsInStock() != null
                        ? new java.util.ArrayList<>(menu.getIngredientsInStock())
                        : new java.util.ArrayList<>());
        args.putStringArrayList(ARG_STEPS,
                menu.getSteps() != null
                        ? new java.util.ArrayList<>(menu.getSteps())
                        : new java.util.ArrayList<>());
        args.putString(ARG_INGREDIENT_NAME, ingredientName);
        args.putInt(ARG_DAYS_LEFT, daysLeft);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_menu_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        String menuName       = args.getString(ARG_MENU_NAME, "");
        List<String> ingredients        = args.getStringArrayList(ARG_INGREDIENTS);
        List<String> ingredientsInStock = args.getStringArrayList(ARG_INGREDIENTS_IN_STOCK);
        List<String> steps              = args.getStringArrayList(ARG_STEPS);
        String ingredientName = args.getString(ARG_INGREDIENT_NAME, "");
        int daysLeft          = args.getInt(ARG_DAYS_LEFT, 0);

        // --- Header ---
        TextView textMenuTitle = view.findViewById(R.id.textMenuTitle);
        textMenuTitle.setText(menuName);

        ImageView btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dismiss());

        // --- Ingredient Chips ---
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupIngredients);
        chipGroup.removeAllViews();

        if (ingredients != null) {
            for (String ing : ingredients) {
                Chip chip = new Chip(requireContext());
                chip.setText(ing);
                chip.setClickable(false);
                chip.setCheckable(false);

                boolean inStock = ingredientsInStock != null && ingredientsInStock.contains(ing);
                if (inStock) {
                    // Green chip — in stock
                    chip.setChipBackgroundColorResource(android.R.color.transparent);
                    chip.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#EAF3DE")));
                    chip.setTextColor(Color.parseColor("#3B6D11"));
                    chip.setChipStrokeColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#3B6D11")));
                    chip.setChipStrokeWidth(1.5f);
                } else {
                    // Grey chip — needs to be sourced
                    chip.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F0F0")));
                    chip.setTextColor(Color.parseColor("#555555"));
                    chip.setChipStrokeWidth(0f);
                }
                chipGroup.addView(chip);
            }
        }

        // --- Steps ---
        LinearLayout stepsContainer = view.findViewById(R.id.stepsContainer);
        stepsContainer.removeAllViews();

        if (steps != null) {
            LayoutInflater li = LayoutInflater.from(requireContext());
            for (int i = 0; i < steps.size(); i++) {
                View stepRow = li.inflate(android.R.layout.simple_list_item_1, stepsContainer, false);
                TextView stepText = stepRow.findViewById(android.R.id.text1);
                stepText.setText((i + 1) + ". " + steps.get(i));
                stepText.setTextSize(14f);
                stepText.setTextColor(Color.parseColor("#333333"));
                stepText.setPadding(0, 6, 0, 6);
                stepsContainer.addView(stepRow);
            }
        }

        // --- Footer Banner ---
        TextView textFooterBanner = view.findViewById(R.id.textFooterBanner);
        textFooterBanner.setText("ใช้ " + ingredientName + " ก่อนหมดอายุใน " + daysLeft + " วัน");
    }
}
