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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.Product;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectProductAdapter extends RecyclerView.Adapter<SelectProductAdapter.ProductSelectViewHolder> {

    public interface OnProductSelectListener {
        void onProductSelected(Product product);
    }

    private final Context context;
    private final List<Product> originalList;
    private List<Product> displayList;
    private final String actionType; // "IN" or "OUT"
    private final OnProductSelectListener listener;

    public SelectProductAdapter(Context context, List<Product> productList, String actionType, OnProductSelectListener listener) {
        this.context = context;
        this.originalList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
        this.displayList = new ArrayList<>(this.originalList);
        this.actionType = actionType != null ? actionType : "IN";
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.originalList.clear();
        if (newList != null) {
            this.originalList.addAll(newList);
        }
        this.displayList = new ArrayList<>(this.originalList);
        notifyDataSetChanged();
    }

    /**
     * Filters list by search keyword against name, sku, and category.
     * Returns the size of the filtered list.
     */
    public int filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayList = new ArrayList<>(originalList);
        } else {
            String lower = query.trim().toLowerCase(Locale.getDefault());
            List<Product> filtered = new ArrayList<>();
            for (Product p : originalList) {
                boolean matchName = p.getName() != null && p.getName().toLowerCase(Locale.getDefault()).contains(lower);
                boolean matchSku = p.getSku() != null && p.getSku().toLowerCase(Locale.getDefault()).contains(lower);
                boolean matchCat = p.getCategory() != null && p.getCategory().toLowerCase(Locale.getDefault()).contains(lower);
                if (matchName || matchSku || matchCat) {
                    filtered.add(p);
                }
            }
            displayList = filtered;
        }
        notifyDataSetChanged();
        return displayList.size();
    }

    public int getDisplayedCount() {
        return displayList.size();
    }

    public int getTotalCount() {
        return originalList.size();
    }

    @NonNull
    @Override
    public ProductSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_product, parent, false);
        return new ProductSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductSelectViewHolder holder, int position) {
        Product p = displayList.get(position);

        holder.tvName.setText(p.getName());
        holder.tvSku.setText(p.getSku() != null && !p.getSku().isEmpty() ? p.getSku() : "NO SKU");
        holder.tvCategory.setText(p.getCategory() != null && !p.getCategory().isEmpty() ? p.getCategory() : "General");
        holder.tvPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", p.getSellPrice()));

        // Stock badge
        if (p.isOutOfStock()) {
            holder.tvStockBadge.setText("0 Units (Out)");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_out_of_stock));
        } else if (p.isLowStock()) {
            holder.tvStockBadge.setText(p.getQuantity() + " Units (Low)");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_low_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_low_stock));
        } else {
            holder.tvStockBadge.setText(p.getQuantity() + " In Stock");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_in_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_in_stock));
        }

        // Action label
        if ("OUT".equalsIgnoreCase(actionType)) {
            holder.tvActionLabel.setText("Stock Out →");
            holder.tvActionLabel.setTextColor(ContextCompat.getColor(context, R.color.status_low_stock));
        } else {
            holder.tvActionLabel.setText("Stock In →");
            holder.tvActionLabel.setTextColor(ContextCompat.getColor(context, R.color.status_in_stock));
        }

        // Image loading
        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    holder.ivThumb.setImageBitmap(bitmap);
                    holder.ivThumb.setVisibility(View.VISIBLE);
                    holder.ivPlaceholder.setVisibility(View.GONE);
                } else {
                    holder.ivThumb.setVisibility(View.GONE);
                    holder.ivPlaceholder.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                holder.ivThumb.setVisibility(View.GONE);
                holder.ivPlaceholder.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivThumb.setVisibility(View.GONE);
            holder.ivPlaceholder.setVisibility(View.VISIBLE);
        }

        // Card click
        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductSelected(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class ProductSelectViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ImageView ivThumb;
        ImageView ivPlaceholder;
        TextView tvName;
        TextView tvSku;
        TextView tvCategory;
        TextView tvPrice;
        TextView tvStockBadge;
        TextView tvActionLabel;

        public ProductSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_select_product_root);
            ivThumb = itemView.findViewById(R.id.iv_select_product_thumb);
            ivPlaceholder = itemView.findViewById(R.id.iv_select_product_placeholder);
            tvName = itemView.findViewById(R.id.tv_select_product_name);
            tvSku = itemView.findViewById(R.id.tv_select_product_sku);
            tvCategory = itemView.findViewById(R.id.tv_select_product_category);
            tvPrice = itemView.findViewById(R.id.tv_select_product_price);
            tvStockBadge = itemView.findViewById(R.id.tv_select_product_stock_badge);
            tvActionLabel = itemView.findViewById(R.id.tv_select_product_action_label);
        }
    }
}
