package com.example.firebasecurd.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.Product;
import com.example.firebasecurd.util.QrBarcodeUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onItemClick(Product product);
        void onStockIn(Product product);
        void onStockOut(Product product);
        void onEdit(Product product);
        void onDelete(Product product);
    }

    private final Context context;
    private List<Product> productList;
    private final OnProductActionListener listener;

    public ProductAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.productList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = productList.get(position);

        holder.tvName.setText(p.getName());
        holder.tvSku.setText(p.getSku());
        holder.tvCategory.setText(p.getCategory());
        holder.tvSellPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", p.getSellPrice()));
        holder.tvBuyPrice.setText(String.format(Locale.getDefault(), "Cost: Rs. %,.2f", p.getBuyPrice()));

        // Stock status badge formatting
        if (p.isOutOfStock()) {
            holder.tvStockBadge.setText("Out of Stock (0)");
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_out_of_stock));
        } else if (p.isLowStock()) {
            holder.tvStockBadge.setText("Low Stock: " + p.getQuantity());
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_low_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_low_stock));
        } else {
            holder.tvStockBadge.setText("In Stock: " + p.getQuantity());
            holder.tvStockBadge.setBackgroundResource(R.drawable.bg_badge_in_stock);
            holder.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.status_in_stock));
        }

        // Product image decode
        if (p.getImageBase64() != null && !p.getImageBase64().trim().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
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

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(p);
        });

        holder.btnStockIn.setOnClickListener(v -> {
            if (listener != null) listener.onStockIn(p);
        });

        holder.btnStockOut.setOnClickListener(v -> {
            if (listener != null) listener.onStockOut(p);
        });

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMore);
            popup.getMenu().add(0, 1, 0, "View Details");
            popup.getMenu().add(0, 2, 1, "Edit Product");
            popup.getMenu().add(0, 3, 2, "Delete Product");
            popup.getMenu().add(0, 4, 3, "🔲 View QR / Shelf Tag");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (listener != null) listener.onItemClick(p);
                    return true;
                } else if (item.getItemId() == 2) {
                    if (listener != null) listener.onEdit(p);
                    return true;
                } else if (item.getItemId() == 3) {
                    if (listener != null) listener.onDelete(p);
                    return true;
                } else if (item.getItemId() == 4) {
                    QrBarcodeUtil.showProductQrDialog(context, p);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb, ivPlaceholder;
        TextView tvName, tvSku, tvCategory, tvSellPrice, tvBuyPrice, tvStockBadge;
        MaterialButton btnStockIn, btnStockOut;
        ImageButton btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_product_thumb);
            ivPlaceholder = itemView.findViewById(R.id.iv_placeholder_icon);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvSku = itemView.findViewById(R.id.tv_product_sku);
            tvCategory = itemView.findViewById(R.id.tv_product_category);
            tvSellPrice = itemView.findViewById(R.id.tv_sell_price);
            tvBuyPrice = itemView.findViewById(R.id.tv_buy_price);
            tvStockBadge = itemView.findViewById(R.id.tv_stock_badge);
            btnStockIn = itemView.findViewById(R.id.btn_quick_stock_in);
            btnStockOut = itemView.findViewById(R.id.btn_quick_stock_out);
            btnMore = itemView.findViewById(R.id.btn_more_options);
        }
    }
}
