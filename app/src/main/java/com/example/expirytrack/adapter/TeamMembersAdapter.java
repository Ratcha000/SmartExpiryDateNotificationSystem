package com.example.expirytrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expirytrack.R;
import com.example.expirytrack.model.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TeamMembersAdapter extends RecyclerView.Adapter<TeamMembersAdapter.ViewHolder> {

    private List<User> teamMembers;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TeamMembersAdapter(List<User> teamMembers) {
        this.teamMembers = teamMembers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = teamMembers.get(position);

        holder.userNameText.setText(user.getDisplayName());
        holder.userEmailText.setText(user.getEmail());

        String roleText = "employee".equals(user.getRole()) ? "👤 พนักงาน" : "👔 หัวหน้า";
        holder.userRoleText.setText(roleText);
    }

    @Override
    public int getItemCount() {
        return teamMembers != null ? teamMembers.size() : 0;
    }

    public void updateList(List<User> newList) {
        this.teamMembers = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        TextView userEmailText;
        TextView userRoleText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            userEmailText = itemView.findViewById(R.id.userEmailText);
            userRoleText = itemView.findViewById(R.id.userRoleText);
        }
    }
}
