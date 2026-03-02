package com.example.expirytrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.model.UsageHistory;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UsageHistoryAdapter extends RecyclerView.Adapter<UsageHistoryAdapter.ViewHolder> {

    private List<UsageHistory> usageHistoryList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public UsageHistoryAdapter(List<UsageHistory> usageHistoryList) {
        this.usageHistoryList = usageHistoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageHistory usage = usageHistoryList.get(position);

        holder.ingredientNameText.setText(usage.getIngredientName());
        holder.performedByText.setText("โดย: " + usage.getPerformedByName());

        // Set action text and color based on action type
        String actionText;
        int actionColor;
        switch (usage.getAction()) {
            case "used":
                actionText = "✅ ใช้แล้ว";
                actionColor = 0xFF4CAF50; // Green
                break;
            case "expired":
                actionText = "❌ หมดอายุ";
                actionColor = 0xFFF44336; // Red
                break;
            case "deleted":
                actionText = "🗑️ ลบแล้ว";
                actionColor = 0xFFFF9800; // Orange
                break;
            default:
                actionText = usage.getAction();
                actionColor = 0xFF666666; // Gray
        }

        holder.actionText.setText(actionText);
        holder.actionText.setTextColor(actionColor);

        // Format and display date
        String dateString = "";
        if (usage.getDate() != null) {
            dateString = dateFormat.format(usage.getDate());
        }
        holder.dateText.setText(dateString);
    }

    @Override
    public int getItemCount() {
        return usageHistoryList != null ? usageHistoryList.size() : 0;
    }

    public void updateList(List<UsageHistory> newList) {
        this.usageHistoryList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ingredientNameText;
        TextView actionText;
        TextView performedByText;
        TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ingredientNameText = itemView.findViewById(R.id.ingredientNameText);
            actionText = itemView.findViewById(R.id.actionText);
            performedByText = itemView.findViewById(R.id.performedByText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
