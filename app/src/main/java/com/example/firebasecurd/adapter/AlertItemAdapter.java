package com.example.firebasecurd.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.Product;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AlertItemAdapter extends RecyclerView.Adapter<AlertItemAdapter.AlertViewHolder> {

    public interface OnRestockClickListener {
        void onRestock(Product product);
    }

    private final Context context;
    private List<Product> alertList;
    private final OnRestockClickListener restockListener;

    public AlertItemAdapter(Context context, List<Product> alertList, OnRestockClickListener restockListener) {
        this.context = context;
        this.alertList = alertList != null ? alertList : new ArrayList<>();
        this.restockListener = restockListener;
    }

    public void updateList(List<Product> newList) {
        this.alertList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert_product, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Product p = alertList.get(position);

        holder.tvName.setText(p.getName());
        holder.tvSku.setText("SKU: " + p.getSku());

        if (p.isOutOfStock()) {
            holder.tvStockBadge.setText("0 Units (Out of Stock)");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            holder.tvStockBadge.setTextColor(context.getColor(R.color.status_out_of_stock));
        } else {
            holder.tvStockBadge.setText(p.getQuantity() + " Units (Low Stock)");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_low_stock);
            holder.tvStockBadge.setTextColor(context.getColor(R.color.status_low_stock));
        }

        holder.tvThreshold.setText("Alert trigger: Below " + p.getMinStockLevel() + " units");

        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                holder.ivPreview.setImageBitmap(b);
                holder.ivPreview.setVisibility(View.VISIBLE);
                holder.ivIcon.setVisibility(View.GONE);
            } catch (Exception e) {
                holder.ivPreview.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivPreview.setVisibility(View.GONE);
            holder.ivIcon.setVisibility(View.VISIBLE);
        }

        holder.btnRestock.setOnClickListener(v -> {
            if (restockListener != null) {
                restockListener.onRestock(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPreview;
        ImageView ivIcon;
        TextView tvName;
        TextView tvSku;
        TextView tvStockBadge;
        TextView tvThreshold;
        MaterialButton btnRestock;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.iv_alert_item_preview);
            ivIcon = itemView.findViewById(R.id.iv_alert_item_icon);
            tvName = itemView.findViewById(R.id.tv_alert_item_name);
            tvSku = itemView.findViewById(R.id.tv_alert_item_sku);
            tvStockBadge = itemView.findViewById(R.id.tv_alert_item_stock_badge);
            tvThreshold = itemView.findViewById(R.id.tv_alert_item_min_threshold);
            btnRestock = itemView.findViewById(R.id.btn_alert_item_restock);
        }
    }
}
