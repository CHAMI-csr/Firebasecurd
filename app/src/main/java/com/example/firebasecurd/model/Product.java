package com.example.firebasecurd.model;

import com.google.firebase.database.Exclude;

public class Product {
    private String id;
    private String sku;
    private String name;
    private String category;
    private double buyPrice;
    private double sellPrice;
    private int quantity;
    private int minStockLevel;
    private String description;
    private String imageBase64;
    private String createdAt;
    private String updatedAt;

    // Required for Firebase
    public Product() {
    }

    public Product(String id, String sku, String name, String category, double buyPrice, double sellPrice,
                   int quantity, int minStockLevel, String description, String imageBase64,
                   String createdAt, String updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Product(String sku, String name, String category, double buyPrice, double sellPrice,
                   int quantity, int minStockLevel, String description, String imageBase64) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
        this.description = description;
        this.imageBase64 = imageBase64;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Exclude
    public boolean isOutOfStock() {
        return quantity <= 0;
    }

    @Exclude
    public boolean isLowStock() {
        return quantity > 0 && quantity <= minStockLevel;
    }

    @Exclude
    public double getProfitMargin() {
        if (buyPrice <= 0) return 0;
        return ((sellPrice - buyPrice) / buyPrice) * 100.0;
    }

    @Exclude
    public double getTotalCostValue() {
        return buyPrice * quantity;
    }

    @Exclude
    public double getTotalRetailValue() {
        return sellPrice * quantity;
    }
}
