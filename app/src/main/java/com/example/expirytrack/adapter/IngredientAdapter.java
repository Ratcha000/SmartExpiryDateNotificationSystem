package com.example.expirytrack.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.model.Ingredient;
import com.google.android.material.chip.Chip;

import java.util.Calendar;
import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {
    private List<Ingredient> ingredients;
    private Context context;
    private OnIngredientActionListener listener;

    public interface OnIngredientActionListener {
        void onAction(Ingredient ingredient, String action);
    }

    public IngredientAdapter(List<Ingredient> ingredients, Context context, OnIngredientActionListener listener) {
        this.ingredients = ingredients;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ingredient ingredient = ingredients.get(position);
        holder.bind(ingredient);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public void setIngredients(List<Ingredient> newIngredients) {
        this.ingredients = newIngredients;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView nameText;
        private Chip categoryChip;
        private TextView expiryDateText;
        private TextView daysLeftText;
        private Button btnUsed;
        private Button btnEdit;
        private Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardIngredient);
            nameText = itemView.findViewById(R.id.textIngredientName);
            categoryChip = itemView.findViewById(R.id.chipCategory);
            expiryDateText = itemView.findViewById(R.id.textExpiryDate);
            daysLeftText = itemView.findViewById(R.id.textDaysLeft);
            btnUsed = itemView.findViewById(R.id.btnUsed);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Ingredient ingredient) {
            nameText.setText(ingredient.getName());
            categoryChip.setText(ingredient.getCategory());

            String expiryFormatted = DateFormat.format("dd/MM/yyyy", ingredient.getExpiryDate()).toString();
            expiryDateText.setText(expiryFormatted);

            long daysLeft = getDaysUntilExpiry(ingredient.getExpiryDate());
            daysLeftText.setText("เหลือ " + daysLeft + " วัน");

            setCardColor(cardView, daysLeft, ingredient.getNotifyDaysBefore());

            btnUsed.setOnClickListener(v -> listener.onAction(ingredient, "used"));
            btnEdit.setOnClickListener(v -> listener.onAction(ingredient, "edit"));
            btnDelete.setOnClickListener(v -> listener.onAction(ingredient, "delete"));
        }

        private long getDaysUntilExpiry(long expiryDate) {
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

        private void setCardColor(CardView card, long daysLeft, int notifyDays) {
            if (daysLeft < 0) {
                card.setCardBackgroundColor(Color.parseColor("#FFCDD2")); // Red
            } else if (daysLeft <= notifyDays) {
                card.setCardBackgroundColor(Color.parseColor("#FFE0B2")); // Orange
            } else {
                card.setCardBackgroundColor(Color.parseColor("#C8E6C9")); // Green
            }
        }
    }
}
