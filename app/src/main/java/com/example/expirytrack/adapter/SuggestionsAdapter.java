package com.example.expirytrack.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.model.IngredientSuggestionGroup;
import com.example.expirytrack.model.MenuSuggestion;
import com.example.expirytrack.util.GeminiService;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.GroupViewHolder> {

    public interface OnMenuClickListener {
        void onMenuClick(MenuSuggestion menu, String ingredientName, int daysLeft);
    }

    // Three distinct dot colors for the 3 menus
    private static final int[] DOT_COLORS = {
            Color.parseColor("#4CAF50"), // green
            Color.parseColor("#009688"), // teal
            Color.parseColor("#2196F3")  // blue
    };

    private final List<IngredientSuggestionGroup> groups;
    private final List<String> allActiveIngredients;
    private final GeminiService geminiService;
    private final OnMenuClickListener menuClickListener;
    private final Context context;

    public SuggestionsAdapter(Context context,
                               List<IngredientSuggestionGroup> groups,
                               List<String> allActiveIngredients,
                               GeminiService geminiService,
                               OnMenuClickListener menuClickListener) {
        this.context = context;
        this.groups = groups;
        this.allActiveIngredients = allActiveIngredients;
        this.geminiService = geminiService;
        this.menuClickListener = menuClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        IngredientSuggestionGroup group = groups.get(position);
        holder.bind(group, position);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout headerRow;
        final TextView badgeDaysLeft;
        final TextView textIngredientName;
        final TextView textExpiryDate;
        final ImageView iconChevron;
        final LinearLayout contentArea;
        final ShimmerFrameLayout shimmerView;
        final LinearLayout errorState;
        final TextView textErrorMessage;
        final MaterialButton retryButton;
        final LinearLayout menuListContainer;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            headerRow = itemView.findViewById(R.id.headerRow);
            badgeDaysLeft = itemView.findViewById(R.id.badgeDaysLeft);
            textIngredientName = itemView.findViewById(R.id.textIngredientName);
            textExpiryDate = itemView.findViewById(R.id.textExpiryDate);
            iconChevron = itemView.findViewById(R.id.iconChevron);
            contentArea = itemView.findViewById(R.id.contentArea);
            shimmerView = itemView.findViewById(R.id.shimmerView);
            errorState = itemView.findViewById(R.id.errorState);
            textErrorMessage = itemView.findViewById(R.id.textErrorMessage);
            retryButton = itemView.findViewById(R.id.retryButton);
            menuListContainer = itemView.findViewById(R.id.menuListContainer);
        }

        void bind(IngredientSuggestionGroup group, int position) {
            // --- Badge ---
            int daysLeft = group.getDaysLeft();
            badgeDaysLeft.setText(daysLeft + " วัน");
            if (daysLeft <= 1) {
                // Red badge
                badgeDaysLeft.setBackgroundResource(R.drawable.days_left_bg_red);
                badgeDaysLeft.setTextColor(Color.parseColor("#A32D2D"));
            } else {
                // Orange badge (default)
                badgeDaysLeft.setBackgroundResource(R.drawable.days_left_bg);
                badgeDaysLeft.setTextColor(Color.parseColor("#854F0B"));
            }

            textIngredientName.setText(group.getIngredientName());
            textExpiryDate.setText("หมดอายุ " + group.getExpiryDateFormatted());

            // --- Expand / collapse state ---
            contentArea.setVisibility(group.isExpanded() ? View.VISIBLE : View.GONE);
            iconChevron.setRotation(group.isExpanded() ? 90f : 0f);

            // Header click
            headerRow.setOnClickListener(v -> {
                int adapterPos = getAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                IngredientSuggestionGroup g = groups.get(adapterPos);

                boolean expanding = !g.isExpanded();
                g.setExpanded(expanding);
                animateChevron(iconChevron, expanding);

                if (expanding) {
                    contentArea.setVisibility(View.VISIBLE);
                    if (!g.isHasLoaded() && !g.isLoading()) {
                        loadMenus(g, adapterPos);
                    } else if (g.isHasLoaded()) {
                        showMenus(g);
                    }
                } else {
                    contentArea.setVisibility(View.GONE);
                }
            });

            // Restore correct inner-content view visibility
            if (group.isExpanded()) {
                if (group.isLoading()) {
                    showShimmer(true);
                    errorState.setVisibility(View.GONE);
                    menuListContainer.setVisibility(View.GONE);
                } else if (group.isHasLoaded()) {
                    showShimmer(false);
                    errorState.setVisibility(View.GONE);
                    menuListContainer.setVisibility(View.VISIBLE);
                    populateMenuList(group);
                } else {
                    showShimmer(false);
                    errorState.setVisibility(View.GONE);
                    menuListContainer.setVisibility(View.GONE);
                }
            }

            // Retry button
            retryButton.setOnClickListener(v -> {
                int adapterPos = getAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                loadMenus(groups.get(adapterPos), adapterPos);
            });
        }

        private void loadMenus(IngredientSuggestionGroup group, int adapterPos) {
            group.setLoading(true);
            group.setHasLoaded(false);

            showShimmer(true);
            errorState.setVisibility(View.GONE);
            menuListContainer.setVisibility(View.GONE);

            geminiService.getMenuSuggestions(
                    group.getIngredientName(),
                    group.getDaysLeft(),
                    allActiveIngredients,
                    new GeminiService.GeminiCallback() {
                        @Override
                        public void onSuccess(List<MenuSuggestion> menus) {
                            group.setMenus(menus);
                            group.setLoading(false);
                            group.setHasLoaded(true);
                            notifyItemChanged(adapterPos);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            group.setLoading(false);
                            group.setHasLoaded(false);
                            // Show error in-place
                            showShimmer(false);
                            menuListContainer.setVisibility(View.GONE);
                            errorState.setVisibility(View.VISIBLE);
                            textErrorMessage.setText(errorMessage);
                        }
                    });
        }

        private void showMenus(IngredientSuggestionGroup group) {
            showShimmer(false);
            errorState.setVisibility(View.GONE);
            menuListContainer.setVisibility(View.VISIBLE);
            populateMenuList(group);
        }

        private void populateMenuList(IngredientSuggestionGroup group) {
            menuListContainer.removeAllViews();
            List<MenuSuggestion> menus = group.getMenus();
            if (menus == null) return;
            for (int i = 0; i < menus.size(); i++) {
                MenuSuggestion menu = menus.get(i);
                View rowView = LayoutInflater.from(context)
                        .inflate(R.layout.item_menu_row, menuListContainer, false);

                View dot = rowView.findViewById(R.id.menuDot);
                dot.getBackground().setTint(DOT_COLORS[i % DOT_COLORS.length]);

                TextView nameView = rowView.findViewById(R.id.textMenuName);
                nameView.setText(menu.getMenuName());

                rowView.setOnClickListener(v -> {
                    if (menuClickListener != null) {
                        menuClickListener.onMenuClick(menu, group.getIngredientName(), group.getDaysLeft());
                    }
                });
                menuListContainer.addView(rowView);
            }
        }

        private void showShimmer(boolean show) {
            if (show) {
                shimmerView.setVisibility(View.VISIBLE);
                shimmerView.startShimmer();
            } else {
                shimmerView.stopShimmer();
                shimmerView.setVisibility(View.GONE);
            }
        }

        private void animateChevron(ImageView chevron, boolean expanding) {
            float from = expanding ? 0f : 90f;
            float to = expanding ? 90f : 0f;
            RotateAnimation rotate = new RotateAnimation(from, to,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(200);
            rotate.setFillAfter(true);
            chevron.startAnimation(rotate);
        }
    }
}
