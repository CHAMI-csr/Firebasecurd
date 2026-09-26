package com.example.firebasecurd.model;

import com.google.firebase.database.Exclude;

public class StockTransaction {
    private String id;
    private String productId;
    private String productName;
    private String type; // "IN" or "OUT"
    private int quantity;
    private int previousStock;
    private int newStock;
    private String reason;
    private String performedBy;
    private String timestamp;

    // Required for Firebase
    public StockTransaction() {
    }

    public StockTransaction(String id, String productId, String productName, String type, int quantity,
                            int previousStock, int newStock, String reason, String performedBy, String timestamp) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = reason;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    public StockTransaction(String productId, String productName, String type, int quantity,
                            int previousStock, int newStock, String reason, String performedBy) {
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = reason;
        this.performedBy = performedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(int previousStock) {
        this.previousStock = previousStock;
    }

    public int getNewStock() {
        return newStock;
    }

    public void setNewStock(int newStock) {
        this.newStock = newStock;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public boolean isStockIn() {
        return "IN".equalsIgnoreCase(type);
    }
}
