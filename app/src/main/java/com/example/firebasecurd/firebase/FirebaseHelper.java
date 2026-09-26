package com.example.firebasecurd.firebase;

import androidx.annotation.NonNull;

import com.example.firebasecurd.model.Product;
import com.example.firebasecurd.model.StockTransaction;
import com.example.firebasecurd.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseDatabase database;
    private final DatabaseReference usersRef;
    private final DatabaseReference productsRef;
    private final DatabaseReference transactionsRef;
    private final DatabaseReference connectedRef;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    public static final String DATABASE_URL = "https://stockmaster-f10f2-default-rtdb.asia-southeast1.firebasedatabase.app";

    private FirebaseHelper() {
        database = FirebaseDatabase.getInstance(DATABASE_URL);
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception ignored) {
            // Persistence can only be set once
        }
        usersRef = database.getReference("users");
        productsRef = database.getReference("products");
        transactionsRef = database.getReference("stock_transactions");
        connectedRef = database.getReference(".info/connected");
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public DatabaseReference getUsersRef() {
        return usersRef;
    }

    public DatabaseReference getProductsRef() {
        return productsRef;
    }

    public DatabaseReference getTransactionsRef() {
        return transactionsRef;
    }

    public DatabaseReference getConnectedRef() {
        return connectedRef;
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ==========================================
    // INITIAL CLOUD SEEDING
    // ==========================================

    public void checkAndSeedDefaultData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    seedDefaultAccounts();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void seedDefaultAccounts() {
        String now = getCurrentDateTime();

        // 1. Default Admin Account
        String adminKey = usersRef.push().getKey();
        if (adminKey != null) {
            User admin = new User(adminKey, "admin", "admin@stockmaster.com", "admin123", "System Administrator", "ADMIN", now);
            usersRef.child(adminKey).setValue(admin);
        }

        // 2. Default Staff Account
        String staffKey = usersRef.push().getKey();
        if (staffKey != null) {
            User staff = new User(staffKey, "staff", "staff@stockmaster.com", "staff123", "Store Operator", "STAFF", now);
            usersRef.child(staffKey).setValue(staff);
        }
    }

    private void seedDefaultProducts() {
        String now = getCurrentDateTime();
        addSeedProduct("SKU-1001", "Wireless Optical Mouse", "Electronics", 1200.0, 1850.0, 35, 10, "Ergonomic 2.4GHz wireless mouse with nano USB receiver.", now);
        addSeedProduct("SKU-1002", "Mechanical Gaming Keyboard", "Electronics", 7500.0, 9900.0, 12, 5, "RGB Backlit blue switch mechanical keyboard.", now);
        addSeedProduct("SKU-1003", "USB-C Fast Charging Cable 2M", "Accessories", 650.0, 1200.0, 4, 15, "Braided nylon high-speed PD fast charging cable. (Low Stock Alert)", now);
        addSeedProduct("SKU-1004", "Laptop Cooling Pad Stand", "Accessories", 2200.0, 3500.0, 0, 5, "Dual silent fan aluminum laptop cooling stand. (Out of Stock Alert)", now);
        addSeedProduct("SKU-1005", "A4 Copy Paper Bundle 500s", "Office Supplies", 1600.0, 2100.0, 60, 20, "80 GSM premium white printing and photocopy paper.", now);
        addSeedProduct("SKU-1006", "External SSD 1TB Portable", "Storage", 18500.0, 24000.0, 18, 5, "USB 3.2 Gen 2 ultra-fast read/write portable SSD.", now);
    }

    private void addSeedProduct(String sku, String name, String category, double buy, double sell, int qty, int min, String desc, String now) {
        String key = productsRef.push().getKey();
        if (key != null) {
            Product p = new Product(key, sku, name, category, buy, sell, qty, min, desc, "", now, now);
            productsRef.child(key).setValue(p);

            if (qty > 0) {
                String tKey = transactionsRef.push().getKey();
                if (tKey != null) {
                    StockTransaction t = new StockTransaction(tKey, key, name, "IN", qty, 0, qty, "Initial Inventory Opening Stock", "admin", now);
                    transactionsRef.child(tKey).setValue(t);
                }
            }
        }
    }

    // ==========================================
    // AUTHENTICATION: USERNAME OR EMAIL
    // ==========================================

    public void loginWithUsernameOrEmail(String usernameOrEmail, String password, AuthCallback callback) {
        final String input = usernameOrEmail.trim().toLowerCase();
        final String pwd = password.trim();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User foundUser = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    User u = child.getValue(User.class);
                    if (u != null) {
                        String uName = u.getUsername() != null ? u.getUsername().trim().toLowerCase() : "";
                        String uEmail = u.getEmail() != null ? u.getEmail().trim().toLowerCase() : "";

                        if (uName.equals(input) || uEmail.equals(input)) {
                            foundUser = u;
                            break;
                        }
                    }
                }

                if (foundUser != null) {
                    if (foundUser.getPassword() != null && foundUser.getPassword().equals(pwd)) {
                        callback.onSuccess(foundUser);
                    } else {
                        callback.onError("Incorrect password. Please try again.");
                    }
                } else {
                    callback.onError("No account found with username or email: " + input);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Cloud database error: " + error.getMessage());
            }
        });
    }

    // ==========================================
    // PRODUCT CRUD
    // ==========================================

    public void addProduct(Product p, String performedBy, OperationCallback callback) {
        String key = productsRef.push().getKey();
        if (key == null) {
            callback.onError("Failed to generate product key");
            return;
        }

        p.setId(key);
        String now = getCurrentDateTime();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);

        final String by = (performedBy != null && !performedBy.trim().isEmpty()) ? performedBy.trim() : "system";

        productsRef.child(key).setValue(p).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (p.getQuantity() > 0) {
                    String tKey = transactionsRef.push().getKey();
                    if (tKey != null) {
                        StockTransaction t = new StockTransaction(tKey, key, p.getName(), "IN", p.getQuantity(), 0, p.getQuantity(), "Initial Inventory Addition", by, now);
                        transactionsRef.child(tKey).setValue(t);
                    }
                }
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to add product");
            }
        });
    }

    public void addProduct(Product p, OperationCallback callback) {
        addProduct(p, "system", callback);
    }

    public void updateProduct(Product p, OperationCallback callback) {
        p.setUpdatedAt(getCurrentDateTime());
        productsRef.child(p.getId()).setValue(p).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Update failed");
            }
        });
    }

    public void deleteProduct(String productId, OperationCallback callback) {
        productsRef.child(productId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Delete failed");
            }
        });
    }

    // ==========================================
    // STOCK ADJUSTMENTS (IN / OUT)
    // ==========================================

    public void adjustStock(Product product, String type, int quantity, String reason, String performedBy, OperationCallback callback) {
        int currentStock = product.getQuantity();
        int newStock;
        if ("IN".equalsIgnoreCase(type)) {
            newStock = currentStock + quantity;
        } else if ("OUT".equalsIgnoreCase(type)) {
            if (currentStock < quantity) {
                callback.onError("Insufficient stock! Available: " + currentStock);
                return;
            }
            newStock = currentStock - quantity;
        } else {
            callback.onError("Invalid adjustment type");
            return;
        }

        String now = getCurrentDateTime();
        product.setQuantity(newStock);
        product.setUpdatedAt(now);

        productsRef.child(product.getId()).child("quantity").setValue(newStock).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String tKey = transactionsRef.push().getKey();
                if (tKey != null) {
                    StockTransaction t = new StockTransaction(tKey, product.getId(), product.getName(), type.toUpperCase(), quantity, currentStock, newStock, reason, performedBy, now);
                    transactionsRef.child(tKey).setValue(t);
                }
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Stock adjustment failed");
            }
        });
    }

    // ==========================================
    // USER MANAGEMENT
    // ==========================================

    public void addUser(User u, OperationCallback callback) {
        String key = usersRef.push().getKey();
        if (key == null) {
            callback.onError("Failed to generate user key");
            return;
        }

        u.setId(key);
        u.setCreatedAt(getCurrentDateTime());
        usersRef.child(key).setValue(u).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to add user");
            }
        });
    }

    public void updateUser(User u, OperationCallback callback) {
        usersRef.child(u.getId()).setValue(u).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to update user");
            }
        });
    }

    public void deleteUser(String userId, OperationCallback callback) {
        usersRef.child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to delete user");
            }
        });
    }

    public void changePassword(String userId, String oldPassword, String newPassword, OperationCallback callback) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User u = snapshot.getValue(User.class);
                if (u != null && u.getPassword() != null && u.getPassword().equals(oldPassword)) {
                    usersRef.child(userId).child("password").setValue(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            callback.onError("Failed to update password");
                        }
                    });
                } else {
                    callback.onError("Current password is incorrect");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Database error: " + error.getMessage());
            }
        });
    }

    public void resetAllData(String currentUserId, OperationCallback callback) {
        transactionsRef.removeValue();
        productsRef.removeValue().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (callback != null) callback.onError(task.getException() != null ? task.getException().getMessage() : "Failed to reset products");
                return;
            }

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        User u = child.getValue(User.class);
                        if (u != null) {
                            String role = u.getRole() != null ? u.getRole().trim().toUpperCase() : "";
                            boolean isCurrentAdmin = child.getKey() != null && child.getKey().equals(currentUserId);
                            // Delete staff accounts, keep all ADMIN accounts (and current user)
                            if (!"ADMIN".equals(role) && !isCurrentAdmin) {
                                child.getRef().removeValue();
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (callback != null) callback.onError(error.getMessage());
                }
            });
        });
    }
}
