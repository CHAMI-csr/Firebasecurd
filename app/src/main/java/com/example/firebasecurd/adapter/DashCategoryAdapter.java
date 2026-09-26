package com.example.firebasecurd.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.CategorySummary;

import java.util.ArrayList;
import java.util.List;

public class DashCategoryAdapter extends RecyclerView.Adapter<DashCategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    private final Context context;
    private List<CategorySummary> list;
    private final OnCategoryClickListener listener;

    public DashCategoryAdapter(Context context, List<CategorySummary> list, OnCategoryClickListener listener) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<CategorySummary> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dash_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySummary item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvCount.setText(item.getCount() + " items");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(item.getName());
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_dash_cat_name);
            tvCount = itemView.findViewById(R.id.tv_dash_cat_count);
        }
    }
}
