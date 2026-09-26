package com.example.firebasecurd.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.firebasecurd.model.Product;
import com.example.firebasecurd.model.StockTransaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportGenerator {

    private static final int PAGE_WIDTH = 595; // A4 standard width in points
    private static final int PAGE_HEIGHT = 842; // A4 standard height in points
    private static final int ITEMS_PER_PAGE = 22;

    public static File generateStockInventoryPdf(Context context, List<Product> products, String exportedBy) {
        if (products == null) return null;

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        // Calculate KPI summaries
        int totalProducts = products.size();
        int totalUnits = 0;
        double totalCost = 0.0;
        double totalRetail = 0.0;

        for (Product p : products) {
            totalUnits += p.getQuantity();
            totalCost += (p.getBuyPrice() * p.getQuantity());
            totalRetail += (p.getSellPrice() * p.getQuantity());
        }
        double totalProfit = totalRetail - totalCost;

        int totalPages = (int) Math.ceil((double) Math.max(1, totalProducts) / ITEMS_PER_PAGE);
        int currentItemIndex = 0;

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // 1. Top Header Banner
            paint.setColor(0xFF0F172A); // Primary Dark
            canvas.drawRect(0, 0, PAGE_WIDTH, 70, paint);

            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(18);
            canvas.drawText("STOCKMASTER PRO", 24, 34, paint);

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(11);
            paint.setColor(0xFF94A3B8); // Slate 400
            canvas.drawText("INVENTORY VALUATION & AUDIT REPORT", 24, 52, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(9);
            paint.setColor(0xFFE2E8F0);
            canvas.drawText("Date: " + dateStr, PAGE_WIDTH - 24, 32, paint);
            canvas.drawText("Exported by: " + (exportedBy != null ? exportedBy : "Admin"), PAGE_WIDTH - 24, 48, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            int startY = 86;

            // 2. Summary KPI Box (Rendered on Page 1)
            if (pageNum == 1) {
                paint.setColor(0xFFF8FAFC); // Slate 50
                canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 54, paint);

                paint.setColor(0xFFCBD5E1);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 54, paint);
                paint.setStyle(Paint.Style.FILL);

                // Column 1: Products & Units
                paint.setColor(0xFF64748B);
                paint.setTextSize(9);
                canvas.drawText("TOTAL PRODUCTS", 36, startY + 18, paint);
                paint.setColor(0xFF0F172A);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(14);
                canvas.drawText(totalProducts + " Items (" + totalUnits + " Units)", 36, startY + 38, paint);

                // Column 2: Total Cost
                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(0xFF64748B);
                paint.setTextSize(9);
                canvas.drawText("TOTAL COST VALUE", 195, startY + 18, paint);
                paint.setColor(0xFF2563EB);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(13);
                canvas.drawText(String.format(Locale.US, "Rs. %,.2f", totalCost), 195, startY + 38, paint);

                // Column 3: Total Retail & Profit
                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(0xFF64748B);
                paint.setTextSize(9);
                canvas.drawText("TOTAL RETAIL (PROJECTED PROFIT)", 355, startY + 18, paint);
                paint.setColor(0xFF16A34A);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(12);
                String profitSign = totalProfit >= 0 ? "+" : "";
                canvas.drawText(String.format(Locale.US, "Rs. %,.2f (%sRs. %,.2f)", totalRetail, profitSign, totalProfit), 355, startY + 38, paint);

                startY += 70;
            }

            // 3. Table Header
            paint.setColor(0xFF1E293B);
            canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 24, paint);

            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(8.5f);

            canvas.drawText("SKU", 28, startY + 15, paint);
            canvas.drawText("PRODUCT NAME", 88, startY + 15, paint);
            canvas.drawText("CATEGORY", 212, startY + 15, paint);
            canvas.drawText("QTY", 298, startY + 15, paint);
            canvas.drawText("COST", 340, startY + 15, paint);
            canvas.drawText("RETAIL", 412, startY + 15, paint);
            canvas.drawText("TOTAL VAL", 482, startY + 15, paint);
            canvas.drawText("STATUS", 546, startY + 15, paint);

            startY += 24;

            // 4. Table Rows
            int rowsDrawn = 0;
            while (currentItemIndex < products.size() && rowsDrawn < ITEMS_PER_PAGE) {
                Product p = products.get(currentItemIndex);

                // Alternating row color
                paint.setColor(rowsDrawn % 2 == 0 ? 0xFFFFFFFF : 0xFFF8FAFC);
                canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 22, paint);

                // Row border bottom
                paint.setColor(0xFFE2E8F0);
                paint.setStrokeWidth(0.5f);
                canvas.drawLine(24, startY + 22, PAGE_WIDTH - 24, startY + 22, paint);

                paint.setColor(0xFF0F172A);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(8f);

                // SKU
                canvas.drawText(p.getSku() != null ? p.getSku() : "-", 28, startY + 14, paint);

                // Truncated Product Name
                String name = p.getName() != null ? p.getName() : "Unnamed";
                if (name.length() > 22) name = name.substring(0, 20) + "..";
                canvas.drawText(name, 88, startY + 14, paint);

                // Truncated Category
                String cat = p.getCategory() != null ? p.getCategory() : "-";
                if (cat.length() > 15) cat = cat.substring(0, 13) + "..";
                canvas.drawText(cat, 212, startY + 14, paint);

                // Qty
                canvas.drawText(String.valueOf(p.getQuantity()), 298, startY + 14, paint);

                // Prices
                canvas.drawText(String.format(Locale.US, "Rs. %,.2f", p.getBuyPrice()), 340, startY + 14, paint);
                canvas.drawText(String.format(Locale.US, "Rs. %,.2f", p.getSellPrice()), 412, startY + 14, paint);
                canvas.drawText(String.format(Locale.US, "Rs. %,.2f", p.getSellPrice() * p.getQuantity()), 482, startY + 14, paint);

                // Stock Status Badge
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                if (p.isOutOfStock()) {
                    paint.setColor(0xFFDC2626); // Red
                    canvas.drawText("OUT", 546, startY + 14, paint);
                } else if (p.isLowStock()) {
                    paint.setColor(0xFFD97706); // Amber
                    canvas.drawText("LOW", 546, startY + 14, paint);
                } else {
                    paint.setColor(0xFF16A34A); // Green
                    canvas.drawText("OK", 546, startY + 14, paint);
                }

                startY += 22;
                currentItemIndex++;
                rowsDrawn++;
            }

            // 5. Page Footer
            paint.setColor(0xFF64748B);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(8);
            canvas.drawText("Confidential • StockMaster Pro Realtime Inventory Cloud System", 24, PAGE_HEIGHT - 20, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Page " + pageNum + " of " + totalPages, PAGE_WIDTH - 24, PAGE_HEIGHT - 20, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            document.finishPage(page);
        }

        try {
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String fileName = "Stock_Inventory_Report_" + System.currentTimeMillis() + ".pdf";
            File file = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.flush();
            fos.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            document.close();
            return null;
        }
    }

    public static File generateLowStockPdf(Context context, List<Product> lowStockProducts, String exportedBy) {
        if (lowStockProducts == null) return null;

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        int totalPages = (int) Math.ceil((double) Math.max(1, lowStockProducts.size()) / 24);
        int currentItemIndex = 0;

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Header Banner (Amber/Red alert tone)
            paint.setColor(0xFF7F1D1D); // Dark Red
            canvas.drawRect(0, 0, PAGE_WIDTH, 70, paint);

            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(18);
            canvas.drawText("STOCKMASTER PRO", 24, 34, paint);

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(11);
            paint.setColor(0xFFFCA5A5); // Light red
            canvas.drawText("CRITICAL LOW STOCK & RE-ORDER REQUISITION SHEET", 24, 52, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(9);
            paint.setColor(0xFFFEE2E2);
            canvas.drawText("Date: " + dateStr, PAGE_WIDTH - 24, 32, paint);
            canvas.drawText("Exported by: " + (exportedBy != null ? exportedBy : "Admin"), PAGE_WIDTH - 24, 48, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            int startY = 88;

            // Table Header
            paint.setColor(0xFF991B1B);
            canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 24, paint);

            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(9);

            canvas.drawText("SKU", 30, startY + 15, paint);
            canvas.drawText("PRODUCT NAME", 100, startY + 15, paint);
            canvas.drawText("CATEGORY", 250, startY + 15, paint);
            canvas.drawText("CURRENT", 340, startY + 15, paint);
            canvas.drawText("ALERT LEVEL", 395, startY + 15, paint);
            canvas.drawText("ORDER SUGGESTION", 460, startY + 15, paint);
            canvas.drawText("STATUS", 540, startY + 15, paint);

            startY += 24;

            int rowsDrawn = 0;
            while (currentItemIndex < lowStockProducts.size() && rowsDrawn < 24) {
                Product p = lowStockProducts.get(currentItemIndex);

                paint.setColor(rowsDrawn % 2 == 0 ? 0xFFFFFFFF : 0xFFFEF2F2);
                canvas.drawRect(24, startY, PAGE_WIDTH - 24, startY + 22, paint);

                paint.setColor(0xFFE2E8F0);
                paint.setStrokeWidth(0.5f);
                canvas.drawLine(24, startY + 22, PAGE_WIDTH - 24, startY + 22, paint);

                paint.setColor(0xFF0F172A);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(8.5f);

                canvas.drawText(p.getSku() != null ? p.getSku() : "-", 30, startY + 14, paint);

                String name = p.getName() != null ? p.getName() : "Unnamed";
                if (name.length() > 24) name = name.substring(0, 22) + "..";
                canvas.drawText(name, 100, startY + 14, paint);

                String cat = p.getCategory() != null ? p.getCategory() : "-";
                if (cat.length() > 16) cat = cat.substring(0, 14) + "..";
                canvas.drawText(cat, 250, startY + 14, paint);

                // Current Qty
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setColor(p.isOutOfStock() ? 0xFFDC2626 : 0xFFD97706);
                canvas.drawText(String.valueOf(p.getQuantity()), 340, startY + 14, paint);

                // Alert Threshold
                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(0xFF0F172A);
                canvas.drawText(String.valueOf(p.getMinStockLevel()), 395, startY + 14, paint);

                // Suggested Order Qty
                int suggestedOrder = Math.max(10, (p.getMinStockLevel() * 3) - p.getQuantity());
                paint.setColor(0xFF2563EB);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("+" + suggestedOrder + " units", 460, startY + 14, paint);

                // Status
                if (p.isOutOfStock()) {
                    paint.setColor(0xFFDC2626);
                    canvas.drawText("OUT", 540, startY + 14, paint);
                } else {
                    paint.setColor(0xFFD97706);
                    canvas.drawText("LOW", 540, startY + 14, paint);
                }

                startY += 22;
                currentItemIndex++;
                rowsDrawn++;
            }

            paint.setColor(0xFF64748B);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(8);
            canvas.drawText("StockMaster Pro Supplier Order Requisition • Urgent Attention", 24, PAGE_HEIGHT - 20, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Page " + pageNum + " of " + totalPages, PAGE_WIDTH - 24, PAGE_HEIGHT - 20, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            document.finishPage(page);
        }

        try {
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String fileName = "Stock_Reorder_Sheet_" + System.currentTimeMillis() + ".pdf";
            File file = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.flush();
            fos.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            document.close();
            return null;
        }
    }

    public static File generateTransactionsCsv(Context context, List<StockTransaction> transactions) {
        if (transactions == null) return null;

        try {
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String fileName = "Stock_Movements_" + System.currentTimeMillis() + ".csv";
            File file = new File(cacheDir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.append("Transaction ID,Timestamp,Movement Type,Product Name,SKU,Quantity,User,Reason / Notes\n");

            for (StockTransaction t : transactions) {
                String id = t.getId() != null ? t.getId() : "";
                String timestamp = t.getTimestamp() != null ? t.getTimestamp() : "";
                String type = t.getType() != null ? t.getType() : "";
                String name = escapeCsv(t.getProductName());
                String sku = escapeCsv(t.getProductId() != null ? t.getProductId() : "");
                int qty = t.getQuantity();
                String user = escapeCsv(t.getPerformedBy() != null ? t.getPerformedBy() : "");
                String reason = escapeCsv(t.getReason());

                writer.append(String.format(Locale.US, "%s,%s,%s,%s,%s,%d,%s,%s\n",
                        id, timestamp, type, name, sku, qty, user, reason));
            }

            writer.flush();
            writer.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static void shareFile(Context context, File file, String mimeType, String title) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "File generation failed", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share " + title));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
