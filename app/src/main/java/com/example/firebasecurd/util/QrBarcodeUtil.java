package com.example.firebasecurd.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class QrBarcodeUtil {

    public static Bitmap generateQrBitmap(String data, int width, int height) {
        if (data == null || data.trim().isEmpty()) {
            data = "N/A";
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
            int matrixWidth = bitMatrix.getWidth();
            int matrixHeight = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.RGB_565);

            int colorDark = 0xFF0F172A; // Deep Navy Slate
            int colorLight = 0xFFFFFFFF; // Pure White

            for (int x = 0; x < matrixWidth; x++) {
                for (int y = 0; y < matrixHeight; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? colorDark : colorLight);
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void showProductQrDialog(Context context, Product product) {
        if (product == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_product_qr, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tv_qr_dialog_product_name);
        TextView tvSku = view.findViewById(R.id.tv_qr_dialog_sku);
        TextView tvCategory = view.findViewById(R.id.tv_qr_dialog_category);
        TextView tvPrice = view.findViewById(R.id.tv_qr_dialog_price);
        ImageView ivQr = view.findViewById(R.id.iv_qr_code_display);
        MaterialButton btnShare = view.findViewById(R.id.btn_qr_dialog_share);
        MaterialButton btnClose = view.findViewById(R.id.btn_qr_dialog_close);

        tvName.setText(product.getName());
        tvSku.setText("SKU: " + product.getSku());
        tvCategory.setText(product.getCategory());
        tvPrice.setText(String.format(Locale.getDefault(), "Retail: Rs. %,.2f", product.getSellPrice()));

        // QR Code encodes SKU which is standard for POS barcode scanners
        Bitmap qrBitmap = generateQrBitmap(product.getSku(), 512, 512);
        if (qrBitmap != null) {
            ivQr.setImageBitmap(qrBitmap);
        }

        AlertDialog dialog = builder.create();

        btnShare.setOnClickListener(v -> {
            if (qrBitmap == null) return;
            try {
                File cacheDir = new File(context.getCacheDir(), "reports");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File qrFile = new File(cacheDir, "QR_" + product.getSku() + ".png");
                FileOutputStream fos = new FileOutputStream(qrFile);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", qrFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Tag: " + product.getName() + " (" + product.getSku() + ")");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Product: " + product.getName() + "\nSKU: " + product.getSku() + "\nRetail Price: " + String.format(Locale.getDefault(), "Rs. %,.2f", product.getSellPrice()));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent, "Share Product QR Tag"));
            } catch (Exception e) {
                Toast.makeText(context, "Error sharing QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int targetWidth = (int) (screenWidth * 0.92);
            dialog.getWindow().setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
