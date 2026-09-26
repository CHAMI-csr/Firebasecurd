package com.example.firebasecurd.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.StockTransaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private List<StockTransaction> transactionList;

    public TransactionAdapter(Context context, List<StockTransaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList != null ? transactionList : new ArrayList<>();
    }

    public void updateList(List<StockTransaction> newList) {
        this.transactionList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockTransaction t = transactionList.get(position);

        holder.tvProductName.setText(t.getProductName());
        holder.tvReason.setText(t.getReason() != null && !t.getReason().isEmpty() ? t.getReason() : "Stock Adjustment");
        holder.tvMeta.setText("By: " + t.getPerformedBy() + " • " + t.getTimestamp());

        boolean isStockIn = t.isStockIn();
        if (isStockIn) {
            holder.flTypeBadge.setBackgroundResource(R.drawable.bg_badge_in_stock);
            holder.ivTypeIcon.setImageResource(R.drawable.ic_stock_in);
            holder.ivTypeIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_in_stock));

            holder.tvQtyChange.setText("+ " + t.getQuantity() + " Units");
            holder.tvQtyChange.setTextColor(ContextCompat.getColor(context, R.color.status_in_stock));
        } else {
            holder.flTypeBadge.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            holder.ivTypeIcon.setImageResource(R.drawable.ic_stock_out);
            holder.ivTypeIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_out_of_stock));

            holder.tvQtyChange.setText("- " + t.getQuantity() + " Units");
            holder.tvQtyChange.setTextColor(ContextCompat.getColor(context, R.color.status_out_of_stock));
        }

        holder.tvStockFlow.setText("Stock: " + t.getPreviousStock() + " → " + t.getNewStock());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flTypeBadge;
        ImageView ivTypeIcon;
        TextView tvProductName, tvReason, tvMeta, tvQtyChange, tvStockFlow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            flTypeBadge = itemView.findViewById(R.id.fl_type_badge);
            ivTypeIcon = itemView.findViewById(R.id.iv_trans_type_icon);
            tvProductName = itemView.findViewById(R.id.tv_trans_product_name);
            tvReason = itemView.findViewById(R.id.tv_trans_reason);
            tvMeta = itemView.findViewById(R.id.tv_trans_meta);
            tvQtyChange = itemView.findViewById(R.id.tv_trans_qty_change);
            tvStockFlow = itemView.findViewById(R.id.tv_trans_stock_flow);
        }
    }
}
