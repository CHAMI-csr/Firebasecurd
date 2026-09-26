package com.example.firebasecurd;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Typeface;
import android.os.Build;
import android.view.WindowInsetsController;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.firebasecurd.adapter.AlertItemAdapter;
import com.example.firebasecurd.adapter.DashCategoryAdapter;
import com.example.firebasecurd.adapter.ProductAdapter;
import com.example.firebasecurd.adapter.SelectProductAdapter;
import com.example.firebasecurd.adapter.TransactionAdapter;
import com.example.firebasecurd.adapter.UserAdapter;
import com.example.firebasecurd.firebase.FirebaseHelper;
import com.example.firebasecurd.model.CategorySummary;
import com.example.firebasecurd.model.Product;
import com.example.firebasecurd.model.StockTransaction;
import com.example.firebasecurd.model.User;
import com.example.firebasecurd.util.NetworkUtil;
import com.example.firebasecurd.util.QrBarcodeUtil;
import com.example.firebasecurd.util.ReportGenerator;
import com.example.firebasecurd.util.SessionManager;
import com.example.firebasecurd.view.StockDonutChartView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.File;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.Manifest;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    // Navigation and Top Bar
    private View viewNetworkIndicatorDot;
    private TextView tvNetworkStatusLabel;
    private TextView tvTopUserBadge;
    private ImageButton btnTopLogout;
    private ImageButton btnTopReports;
    private ImageButton btnTopNotifications;
    private TextView tvNotificationBadge;
    private View layoutMainOfflineBanner;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAddProduct;

    // Inventory Views
    private EditText etInventorySearch;
    private ImageButton btnClearSearch;
    private ImageButton btnScanBarcode;
    private ChipGroup chipGroupCategories;
    private ChipGroup chipGroupStockStatus;
    private TextView tvInventoryCountLabel;
    private RecyclerView rvInventoryProducts;
    private View layoutInventoryEmpty;
    private MaterialButton btnEmptyAddProduct;
    private ProductAdapter productAdapter;
    private String selectedCategory = "All";
    private String selectedStockStatus = "All Stock";

    // Barcode Scanner Launcher
    private TextInputEditText currentSkuInputForScan = null;
    private final ActivityResultLauncher<ScanOptions> barcodeScannerLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result != null && result.getContents() != null) {
                    String scannedCode = result.getContents().trim();
                    if (currentSkuInputForScan != null) {
                        currentSkuInputForScan.setText(scannedCode);
                        currentSkuInputForScan = null;
                        Toast.makeText(this, "Scanned SKU: " + scannedCode, Toast.LENGTH_SHORT).show();
                    } else if (etInventorySearch != null) {
                        etInventorySearch.setText(scannedCode);
                        Toast.makeText(this, "Barcode Found: " + scannedCode, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Dashboard Charts
    private StockDonutChartView chartStockValuation;
    private LinearLayout layoutChartLegends;
    private TextView tvChartStockInFlow;
    private TextView tvChartStockOutFlow;
    private TextView btnDashExportReports;

    // Tab Views
    private View viewDashboard;
    private View viewInventory;
    private View viewHistory;
    private View viewAdmin;

    // Dashboard Views
    private TextView tvDashAvatarInitial;
    private TextView tvDashGreeting;
    private TextView tvDashDate;
    private TextView tvDashUserRole;
    private TextView tvDashHealthScoreBadge;
    private TextView tvMetricRetailValue, tvMetricCostValue, tvMetricProfitMargin;
    private View barStockHealthy, barStockLow, barStockOut;
    private TextView tvLegendHealthy, tvLegendLow, tvLegendOut;
    private View btnTileAddProduct, btnTileStockIn, btnTileStockOut, btnTileAlerts;
    private TextView tvTileAlertsTitle;
    private TextView tvMetricTotalProducts, tvMetricTotalUnits, tvMetricLowStock, tvMetricOutStock;
    private RecyclerView rvDashCategories;
    private DashCategoryAdapter dashCategoryAdapter;
    private TextView tvDashSeeAllInventory;
    private View cardLowStockBanner;
    private MaterialButton btnViewLowStock;
    private TextView tvDashSeeAllHistory;
    private RecyclerView rvDashRecentTransactions;
    private TransactionAdapter dashRecentAdapter;

    // History Views
    private ChipGroup chipGroupHistoryType;
    private TextView tvHistoryCountLabel;
    private RecyclerView rvHistoryTransactions;
    private View layoutHistoryEmpty;
    private TransactionAdapter fullHistoryAdapter;
    private String selectedHistoryType = "ALL";

    // Admin & Accounts Views
    private TextView tvAdminProfileInitial, tvAdminProfileName, tvAdminProfileUsername, tvAdminProfileRole;
    private View layoutUserManagementSection;
    private TextView tvStaffRestrictedNotice;
    private MaterialButton btnAdminAddUser;
    private RecyclerView rvAdminUsers;
    private UserAdapter userAdapter;
    private View rowChangePassword, rowResetData, dividerResetData, rowLogout;

    // In-memory synced data lists from Firebase
    private final List<Product> masterProductList = new ArrayList<>();
    private final List<StockTransaction> masterTransactionList = new ArrayList<>();
    private final List<User> masterUserList = new ArrayList<>();

    // Dialog Image Picker & Camera Handler
    private ImageView currentDialogImageView;
    private ImageView currentDialogPlaceholder;
    private TextView currentDialogImageStatus;
    private TextView currentDialogRemoveImage;
    private String currentDialogBase64 = null;
    private Uri currentCameraPhotoUri = null;
    private File currentCameraPhotoFile = null;
    private String currentCameraPhotoPath = null;
    private AlertDialog currentProductDialog = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // Network callback
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentCameraPhotoPath = savedInstanceState.getString("current_camera_photo_path");
            if (currentCameraPhotoPath != null) {
                currentCameraPhotoFile = new File(currentCameraPhotoPath);
                try {
                    currentCameraPhotoUri = FileProvider.getUriForFile(
                            this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            currentCameraPhotoFile
                    );
                } catch (Exception ignored) {}
            }
            currentDialogBase64 = savedInstanceState.getString("current_dialog_base64");
        }

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Configure edge-to-edge with dark status bar and light icons
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        try {
            androidx.core.view.WindowInsetsControllerCompat controller = 
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
            }
        } catch (Exception ignored) {}

        // Handle Window Insets dynamically for Status Bar & Nav Bar
        View layoutTopBarContent = findViewById(R.id.layout_top_bar_content);
        View rootCoordinator = findViewById(R.id.main_root_coordinator);
        View bottomNavView = findViewById(R.id.bottom_navigation);
        if (rootCoordinator != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootCoordinator, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (layoutTopBarContent != null) {
                    layoutTopBarContent.setPadding(
                            layoutTopBarContent.getPaddingLeft(),
                            insets.top,
                            layoutTopBarContent.getPaddingRight(),
                            layoutTopBarContent.getPaddingBottom()
                    );
                }
                if (bottomNavView != null) {
                    bottomNavView.setPadding(
                            bottomNavView.getPaddingLeft(),
                            bottomNavView.getPaddingTop(),
                            bottomNavView.getPaddingRight(),
                            insets.bottom
                    );
                }
                View fab = findViewById(R.id.fab_main_add_product);
                if (fab != null && fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                    int baseMargin = (int) (96 * getResources().getDisplayMetrics().density);
                    lp.bottomMargin = baseMargin + insets.bottom;
                    fab.setLayoutParams(lp);
                }
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(rootCoordinator);
        }

        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.checkAndSeedDefaultData();

        setupImagePickerLauncher();
        initViews();
        setupNavigation();
        setupDashboardTab();
        setupInventoryTab();
        setupHistoryTab();
        setupAdminTab();

        setupNetworkMonitoring();
        setupFirebaseRealtimeListeners();
    }

    private void setupNetworkMonitoring() {
        boolean initialOnline = NetworkUtil.isNetworkAvailable(this);
        updateNetworkStatusUI(initialOnline);

        networkCallback = NetworkUtil.registerNetworkListener(this, this::updateNetworkStatusUI);

        firebaseHelper.getConnectedRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null) {
                    updateNetworkStatusUI(connected);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateNetworkStatusUI(boolean isOnline) {
        runOnUiThread(() -> {
            if (isOnline) {
                viewNetworkIndicatorDot.setBackgroundResource(R.drawable.bg_badge_in_stock);
                tvNetworkStatusLabel.setText("Online (Synced)");
                tvNetworkStatusLabel.setTextColor(ContextCompat.getColor(this, R.color.status_in_stock));
                layoutMainOfflineBanner.setVisibility(View.GONE);
            } else {
                viewNetworkIndicatorDot.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
                tvNetworkStatusLabel.setText("Offline (No Internet)");
                tvNetworkStatusLabel.setTextColor(ContextCompat.getColor(this, R.color.status_out_of_stock));
                layoutMainOfflineBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupFirebaseRealtimeListeners() {
        // 1. Realtime Products Sync
        firebaseHelper.getProductsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterProductList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product p = child.getValue(Product.class);
                    if (p != null) {
                        p.setId(child.getKey());
                        masterProductList.add(p);
                    }
                }
                Collections.reverse(masterProductList);
                refreshDashboard();
                refreshInventory();
                refreshNotificationBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Realtime Transactions Sync
        firebaseHelper.getTransactionsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterTransactionList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    StockTransaction t = child.getValue(StockTransaction.class);
                    if (t != null) {
                        t.setId(child.getKey());
                        masterTransactionList.add(t);
                    }
                }
                Collections.reverse(masterTransactionList);
                refreshDashboard();
                refreshHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Realtime Users Sync (for Admin tab)
        firebaseHelper.getUsersRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterUserList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User u = child.getValue(User.class);
                    if (u != null) {
                        u.setId(child.getKey());
                        masterUserList.add(u);
                    }
                }
                refreshAdmin();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        processAndSetImage(result.getData().getData());
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    boolean photoProcessed = false;

                    // 1. Direct photo file check (preferred & highest quality)
                    if (currentCameraPhotoFile != null && currentCameraPhotoFile.exists() && currentCameraPhotoFile.length() > 0) {
                        processAndSetImage(Uri.fromFile(currentCameraPhotoFile));
                        photoProcessed = true;
                    }
                    // 2. Content URI check
                    else if (currentCameraPhotoUri != null) {
                        try (InputStream is = getContentResolver().openInputStream(currentCameraPhotoUri)) {
                            if (is != null && is.available() > 0) {
                                processAndSetImage(currentCameraPhotoUri);
                                photoProcessed = true;
                            }
                        } catch (Exception ignored) {}
                    }

                    // 3. Fallback: Intent extra Bitmap (for cameras that return thumbnail)
                    if (!photoProcessed && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") instanceof Bitmap) {
                            Bitmap bitmap = (Bitmap) extras.get("data");
                            processAndSetBitmap(bitmap);
                            photoProcessed = true;
                        }
                    }

                    if (!photoProcessed && result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Could not load photo from camera", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        launchCamera();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            launchCamera();
                        } else {
                            Toast.makeText(this, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void checkCameraPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                storageDir = getExternalCacheDir();
            }
            if (storageDir == null) {
                storageDir = getCacheDir();
            }
            File cameraDir = new File(storageDir, "camera_photos");
            if (!cameraDir.exists()) {
                cameraDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentCameraPhotoFile = new File(cameraDir, "IMG_" + timeStamp + ".jpg");
            currentCameraPhotoPath = currentCameraPhotoFile.getAbsolutePath();

            currentCameraPhotoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    currentCameraPhotoFile
            );

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentCameraPhotoUri);
            takePictureIntent.setClipData(ClipData.newRawUri("photo", currentCameraPhotoUri));
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Explicitly grant URI permission to resolving camera applications
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, currentCameraPhotoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (takePictureIntent.resolveActivity(getPackageManager()) != null || !resInfoList.isEmpty()) {
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, "No camera application found on this device", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot start camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Product Image"));
    }

    private void showImageSourceOptionsDialog() {
        List<String> options = new ArrayList<>();
        options.add("Take Photo with Camera");
        options.add("Choose from Gallery");
        if (currentDialogBase64 != null && !currentDialogBase64.isEmpty()) {
            options.add("Remove Photo");
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Product Photo")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        launchGalleryPicker();
                    } else if (which == 2) {
                        clearDialogImage();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearDialogImage() {
        currentDialogBase64 = null;
        if (currentDialogImageView != null) {
            currentDialogImageView.setImageDrawable(null);
        }
        if (currentDialogPlaceholder != null) {
            currentDialogPlaceholder.setVisibility(View.VISIBLE);
        }
        if (currentDialogImageStatus != null) {
            currentDialogImageStatus.setText("Camera or gallery photo");
        }
        if (currentDialogRemoveImage != null) {
            currentDialogRemoveImage.setVisibility(View.GONE);
        }
    }

    private void processAndSetImage(Uri imageUri) {
        if (imageUri == null) return;
        try {
            // Step 1: Decode image dimensions first to prevent OutOfMemoryError on high-res camera photos
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                if (is == null) {
                    Toast.makeText(this, "Unable to read captured photo", Toast.LENGTH_SHORT).show();
                    return;
                }
                BitmapFactory.decodeStream(is, null, boundsOptions);
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                Toast.makeText(this, "Invalid image format", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 2: Compute downsampling sample size
            final int MAX_DIMENSION = 800;
            int sampleSize = 1;
            int halfWidth = boundsOptions.outWidth / 2;
            int halfHeight = boundsOptions.outHeight / 2;
            while ((halfWidth / sampleSize) >= MAX_DIMENSION || (halfHeight / sampleSize) >= MAX_DIMENSION) {
                sampleSize *= 2;
            }

            // Step 3: Decode bitmap using calculated sample size and RGB_565 (50% less RAM)
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap sampledBitmap;
            try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                sampledBitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
            }

            if (sampledBitmap == null) {
                Toast.makeText(this, "Failed to decode photo", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 4: Correct EXIF orientation
            int rotationDegrees = 0;
            try (InputStream exifStream = getContentResolver().openInputStream(imageUri)) {
                if (exifStream != null) {
                    ExifInterface exif = new ExifInterface(exifStream);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        rotationDegrees = 90;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        rotationDegrees = 180;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        rotationDegrees = 270;
                    }
                }
            } catch (Throwable ignored) {}

            Bitmap orientedBitmap = sampledBitmap;
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                orientedBitmap = Bitmap.createBitmap(sampledBitmap, 0, 0,
                        sampledBitmap.getWidth(), sampledBitmap.getHeight(), matrix, true);
                if (orientedBitmap != sampledBitmap) {
                    sampledBitmap.recycle();
                }
            }

            // Step 5: Scale to standard product photo size (max 600px)
            Bitmap finalBitmap = scaleBitmapDown(orientedBitmap, 600);
            if (finalBitmap != orientedBitmap) {
                orientedBitmap.recycle();
            }

            // Step 6: Compress to JPEG and convert to Base64 string for Firebase storage
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            currentDialogBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

            // Step 7: Update UI preview in the active dialog
            if (currentDialogImageView != null) {
                currentDialogImageView.setImageBitmap(finalBitmap);
            }
            if (currentDialogPlaceholder != null) {
                currentDialogPlaceholder.setVisibility(View.GONE);
            }
            if (currentDialogImageStatus != null) {
                currentDialogImageStatus.setText("Photo attached (" + (bytes.length / 1024) + " KB)");
            }
            if (currentDialogRemoveImage != null) {
                currentDialogRemoveImage.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "Photo attached successfully!", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "Failed to process photo: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndSetBitmap(Bitmap originalBitmap) {
        if (originalBitmap == null) return;
        try {
            Bitmap finalBitmap = scaleBitmapDown(originalBitmap, 600);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            currentDialogBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

            if (currentDialogImageView != null) {
                currentDialogImageView.setImageBitmap(finalBitmap);
            }
            if (currentDialogPlaceholder != null) {
                currentDialogPlaceholder.setVisibility(View.GONE);
            }
            if (currentDialogImageStatus != null) {
                currentDialogImageStatus.setText("Photo attached (" + (bytes.length / 1024) + " KB)");
            }
            if (currentDialogRemoveImage != null) {
                currentDialogRemoveImage.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "Photo attached successfully!", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "Failed to process photo: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap;
        }
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = Math.max(1, (int) (resizedHeight * (float) originalWidth / (float) originalHeight));
        } else {
            resizedWidth = maxDimension;
            resizedHeight = Math.max(1, (int) (resizedWidth * (float) originalHeight / (float) originalWidth));
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true);
    }

    private void initViews() {
        viewNetworkIndicatorDot = findViewById(R.id.view_network_indicator_dot);
        tvNetworkStatusLabel = findViewById(R.id.tv_network_status_label);
        tvTopUserBadge = findViewById(R.id.tv_top_user_badge);
        btnTopLogout = findViewById(R.id.btn_top_logout);
        btnTopReports = findViewById(R.id.btn_top_reports);
        btnTopNotifications = findViewById(R.id.btn_top_notifications);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
        layoutMainOfflineBanner = findViewById(R.id.layout_main_offline_banner);
        bottomNav = findViewById(R.id.bottom_navigation);
        fabAddProduct = findViewById(R.id.fab_main_add_product);

        viewDashboard = findViewById(R.id.view_dashboard);
        viewInventory = findViewById(R.id.view_inventory);
        viewHistory = findViewById(R.id.view_history);
        viewAdmin = findViewById(R.id.view_admin);

        tvTopUserBadge.setText(sessionManager.getUserRole() + ": " + sessionManager.getUsername());

        btnTopLogout.setOnClickListener(v -> confirmLogout());
        if (btnTopReports != null) {
            btnTopReports.setOnClickListener(v -> showExportReportsDialog());
        }
        if (btnTopNotifications != null) {
            btnTopNotifications.setOnClickListener(v -> showLowStockAlertsSheet());
        }
        fabAddProduct.setOnClickListener(v -> showAddEditProductDialog(null));
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                switchTab(0);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                switchTab(1);
                return true;
            } else if (itemId == R.id.nav_history) {
                switchTab(2);
                return true;
            } else if (itemId == R.id.nav_admin) {
                switchTab(3);
                return true;
            }
            return false;
        });
    }

    private void switchTab(int tabIndex) {
        viewDashboard.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        viewInventory.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        viewHistory.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        viewAdmin.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        // FAB is only visible on Inventory tab (Tab 1), avoiding covering Dashboard metrics
        fabAddProduct.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);

        if (tabIndex == 0) refreshDashboard();
        if (tabIndex == 1) refreshInventory();
        if (tabIndex == 2) refreshHistory();
        if (tabIndex == 3) refreshAdmin();
    }

    private void applyDialogWindowStyles(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int targetWidth = (int) (screenWidth * 0.94);
        dialog.getWindow().setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    // ==========================================
    // TAB 1: DASHBOARD
    // ==========================================

    private void setupDashboardTab() {
        tvDashAvatarInitial = viewDashboard.findViewById(R.id.tv_dash_avatar_initial);
        tvDashGreeting = viewDashboard.findViewById(R.id.tv_dash_greeting);
        tvDashDate = viewDashboard.findViewById(R.id.tv_dash_date);
        tvDashUserRole = viewDashboard.findViewById(R.id.tv_dash_user_role);
        tvDashHealthScoreBadge = viewDashboard.findViewById(R.id.tv_dash_health_score_badge);

        tvMetricRetailValue = viewDashboard.findViewById(R.id.tv_metric_retail_value);
        tvMetricCostValue = viewDashboard.findViewById(R.id.tv_metric_cost_value);
        tvMetricProfitMargin = viewDashboard.findViewById(R.id.tv_metric_profit_margin);

        barStockHealthy = viewDashboard.findViewById(R.id.bar_stock_healthy);
        barStockLow = viewDashboard.findViewById(R.id.bar_stock_low);
        barStockOut = viewDashboard.findViewById(R.id.bar_stock_out);
        tvLegendHealthy = viewDashboard.findViewById(R.id.tv_legend_healthy);
        tvLegendLow = viewDashboard.findViewById(R.id.tv_legend_low);
        tvLegendOut = viewDashboard.findViewById(R.id.tv_legend_out);

        btnTileAddProduct = viewDashboard.findViewById(R.id.btn_tile_add_product);
        btnTileStockIn = viewDashboard.findViewById(R.id.btn_tile_stock_in);
        btnTileStockOut = viewDashboard.findViewById(R.id.btn_tile_stock_out);
        btnTileAlerts = viewDashboard.findViewById(R.id.btn_tile_alerts);
        tvTileAlertsTitle = viewDashboard.findViewById(R.id.tv_tile_alerts_title);

        tvMetricTotalProducts = viewDashboard.findViewById(R.id.tv_metric_total_products);
        tvMetricTotalUnits = viewDashboard.findViewById(R.id.tv_metric_total_units);
        tvMetricLowStock = viewDashboard.findViewById(R.id.tv_metric_low_stock);
        tvMetricOutStock = viewDashboard.findViewById(R.id.tv_metric_out_stock);

        rvDashCategories = viewDashboard.findViewById(R.id.rv_dash_categories);
        tvDashSeeAllInventory = viewDashboard.findViewById(R.id.tv_dash_see_all_inventory);

        cardLowStockBanner = viewDashboard.findViewById(R.id.card_low_stock_banner);
        btnViewLowStock = viewDashboard.findViewById(R.id.btn_view_low_stock);
        tvDashSeeAllHistory = viewDashboard.findViewById(R.id.tv_dash_see_all_history);
        rvDashRecentTransactions = viewDashboard.findViewById(R.id.rv_dash_recent_transactions);

        // Header info
        String initial = !sessionManager.getFullName().isEmpty()
                ? sessionManager.getFullName().substring(0, 1).toUpperCase()
                : "A";
        tvDashAvatarInitial.setText(initial);
        tvDashGreeting.setText("Hello, " + sessionManager.getFullName() + " 👋");
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        tvDashDate.setText(sdf.format(new Date()));
        tvDashUserRole.setText(sessionManager.getUserRole());

        // Category pills carousel setup
        rvDashCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dashCategoryAdapter = new DashCategoryAdapter(this, new ArrayList<>(), categoryName -> {
            bottomNav.setSelectedItemId(R.id.nav_inventory);
            selectedCategory = categoryName;
            filterProducts();
        });
        rvDashCategories.setAdapter(dashCategoryAdapter);

        // Quick action tiles
        btnTileAddProduct.setOnClickListener(v -> showAddEditProductDialog(null));
        btnTileStockIn.setOnClickListener(v -> showProductSelectDialog("IN"));
        btnTileStockOut.setOnClickListener(v -> showProductSelectDialog("OUT"));
        btnTileAlerts.setOnClickListener(v -> showLowStockAlertsSheet());

        tvDashSeeAllInventory.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_inventory));
        btnViewLowStock.setOnClickListener(v -> showLowStockAlertsSheet());

        // Visual Charts views
        chartStockValuation = viewDashboard.findViewById(R.id.chart_stock_valuation);
        layoutChartLegends = viewDashboard.findViewById(R.id.layout_chart_legends);
        tvChartStockInFlow = viewDashboard.findViewById(R.id.tv_chart_stock_in_flow);
        tvChartStockOutFlow = viewDashboard.findViewById(R.id.tv_chart_stock_out_flow);
        btnDashExportReports = viewDashboard.findViewById(R.id.btn_dash_export_reports);

        if (btnDashExportReports != null) {
            btnDashExportReports.setOnClickListener(v -> showExportReportsDialog());
        }

        // Recent movements
        rvDashRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        dashRecentAdapter = new TransactionAdapter(this, new ArrayList<>());
        rvDashRecentTransactions.setAdapter(dashRecentAdapter);
        tvDashSeeAllHistory.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_history));
    }

    private void refreshDashboard() {
        int totalProducts = masterProductList.size();
        int totalUnits = 0;
        double retailVal = 0.0;
        double costVal = 0.0;
        int lowStockCount = 0;
        int outStockCount = 0;
        int healthyCount = 0;

        Map<String, Integer> categoryCountMap = new HashMap<>();

        for (Product p : masterProductList) {
            totalUnits += p.getQuantity();
            retailVal += p.getTotalRetailValue();
            costVal += p.getTotalCostValue();

            if (p.isOutOfStock()) {
                outStockCount++;
            } else if (p.isLowStock()) {
                lowStockCount++;
            } else {
                healthyCount++;
            }

            String cat = p.getCategory() != null && !p.getCategory().trim().isEmpty() ? p.getCategory().trim() : "General";
            categoryCountMap.put(cat, categoryCountMap.getOrDefault(cat, 0) + 1);
        }

        tvMetricTotalProducts.setText(String.valueOf(totalProducts));
        tvMetricTotalUnits.setText(String.valueOf(totalUnits));
        tvMetricRetailValue.setText(String.format(Locale.getDefault(), "Rs. %,.2f", retailVal));
        tvMetricCostValue.setText(String.format(Locale.getDefault(), "Cost: Rs. %,.2f", costVal));

        double margin = costVal > 0 ? ((retailVal - costVal) / costVal) * 100.0 : 0.0;
        tvMetricProfitMargin.setText(String.format(Locale.getDefault(), "Est. Margin: +%.1f%%", margin));

        tvMetricLowStock.setText(lowStockCount + " Items");
        tvMetricOutStock.setText(outStockCount + " Items");

        int totalAlerts = lowStockCount + outStockCount;
        tvTileAlertsTitle.setText(totalAlerts > 0 ? "Reorders (" + totalAlerts + ")" : "Reorders");

        // Segmented Health Bar
        if (totalProducts > 0) {
            float healthyWeight = ((float) healthyCount / totalProducts) * 100f;
            float lowWeight = ((float) lowStockCount / totalProducts) * 100f;
            float outWeight = ((float) outStockCount / totalProducts) * 100f;

            setBarWeight(barStockHealthy, healthyWeight > 0 ? Math.max(5, healthyWeight) : 0);
            setBarWeight(barStockLow, lowWeight > 0 ? Math.max(5, lowWeight) : 0);
            setBarWeight(barStockOut, outWeight > 0 ? Math.max(5, outWeight) : 0);

            tvLegendHealthy.setText("● Healthy: " + healthyCount);
            tvLegendLow.setText("● Low: " + lowStockCount);
            tvLegendOut.setText("● Out: " + outStockCount);

            int healthPct = Math.round(((float) healthyCount / totalProducts) * 100f);
            tvDashHealthScoreBadge.setText("● " + healthPct + "% Stock Health");
        } else {
            tvDashHealthScoreBadge.setText("● 0 Items");
        }

        // Low stock reorder alert banner
        if (totalAlerts > 0) {
            cardLowStockBanner.setVisibility(View.VISIBLE);
        } else {
            cardLowStockBanner.setVisibility(View.GONE);
        }

        // Category pills
        List<CategorySummary> categorySummaries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCountMap.entrySet()) {
            categorySummaries.add(new CategorySummary(entry.getKey(), entry.getValue()));
        }
        dashCategoryAdapter.updateList(categorySummaries);

        // Update Visual Donut Chart and velocity flows
        updateDashboardCharts(retailVal);

        // Recent 5 transactions
        List<StockTransaction> recent = new ArrayList<>();
        int count = Math.min(5, masterTransactionList.size());
        for (int i = 0; i < count; i++) {
            recent.add(masterTransactionList.get(i));
        }
        dashRecentAdapter.updateList(recent);
    }

    private void setBarWeight(View view, float weight) {
        if (view == null) return;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = weight;
        view.setLayoutParams(params);
        view.setVisibility(weight > 0 ? View.VISIBLE : View.GONE);
    }

    // ==========================================
    // TAB 2: INVENTORY & PRODUCTS
    // ==========================================

    private void setupInventoryTab() {
        etInventorySearch = viewInventory.findViewById(R.id.et_inventory_search);
        btnClearSearch = viewInventory.findViewById(R.id.btn_clear_search);
        chipGroupCategories = viewInventory.findViewById(R.id.chip_group_categories);
        chipGroupStockStatus = viewInventory.findViewById(R.id.chip_group_stock_status);
        tvInventoryCountLabel = viewInventory.findViewById(R.id.tv_inventory_count_label);
        rvInventoryProducts = viewInventory.findViewById(R.id.rv_inventory_products);
        layoutInventoryEmpty = viewInventory.findViewById(R.id.layout_inventory_empty);
        btnEmptyAddProduct = viewInventory.findViewById(R.id.btn_empty_add_product);

        rvInventoryProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, new ArrayList<>(), new ProductAdapter.OnProductActionListener() {
            @Override
            public void onItemClick(Product product) {
                showProductDetailsDialog(product);
            }

            @Override
            public void onStockIn(Product product) {
                showStockAdjustDialog(product, "IN");
            }

            @Override
            public void onStockOut(Product product) {
                showStockAdjustDialog(product, "OUT");
            }

            @Override
            public void onEdit(Product product) {
                showAddEditProductDialog(product);
            }

            @Override
            public void onDelete(Product product) {
                confirmDeleteProduct(product);
            }
        });
        rvInventoryProducts.setAdapter(productAdapter);

        btnEmptyAddProduct.setOnClickListener(v -> showAddEditProductDialog(null));

        etInventorySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etInventorySearch.setText(""));

        btnScanBarcode = viewInventory.findViewById(R.id.btn_scan_barcode);
        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> launchBarcodeScanner(null));
        }

        chipGroupStockStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_stock_all) {
                    selectedStockStatus = "All Stock";
                } else if (id == R.id.chip_stock_in) {
                    selectedStockStatus = "In Stock";
                } else if (id == R.id.chip_stock_low) {
                    selectedStockStatus = "Low Stock";
                } else if (id == R.id.chip_stock_out) {
                    selectedStockStatus = "Out of Stock";
                }
                filterProducts();
            }
        });
    }

    private void refreshCategoryChips() {
        chipGroupCategories.removeAllViews();

        Set<String> catSet = new HashSet<>();
        for (Product p : masterProductList) {
            if (p.getCategory() != null && !p.getCategory().trim().isEmpty()) {
                catSet.add(p.getCategory().trim());
            }
        }
        List<String> categories = new ArrayList<>(catSet);
        Collections.sort(categories);
        categories.add(0, "All");

        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);

            if (cat.equalsIgnoreCase(selectedCategory)) {
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategory = cat;
                    filterProducts();
                }
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void filterProducts() {
        String query = etInventorySearch.getText() != null ? etInventorySearch.getText().toString().trim().toLowerCase() : "";
        List<Product> filtered = new ArrayList<>();

        for (Product p : masterProductList) {
            boolean matchesQuery = query.isEmpty() ||
                    (p.getName() != null && p.getName().toLowerCase().contains(query)) ||
                    (p.getSku() != null && p.getSku().toLowerCase().contains(query)) ||
                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(query));

            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All") ||
                    (p.getCategory() != null && p.getCategory().equalsIgnoreCase(selectedCategory));

            boolean matchesStock = true;
            if ("In Stock".equalsIgnoreCase(selectedStockStatus)) {
                matchesStock = !p.isOutOfStock() && !p.isLowStock();
            } else if ("Low Stock".equalsIgnoreCase(selectedStockStatus)) {
                matchesStock = p.isLowStock();
            } else if ("Out of Stock".equalsIgnoreCase(selectedStockStatus)) {
                matchesStock = p.isOutOfStock();
            }

            if (matchesQuery && matchesCategory && matchesStock) {
                filtered.add(p);
            }
        }

        productAdapter.updateList(filtered);
        tvInventoryCountLabel.setText("Showing " + filtered.size() + " Products (Cloud Synced)");

        if (filtered.isEmpty()) {
            layoutInventoryEmpty.setVisibility(View.VISIBLE);
            rvInventoryProducts.setVisibility(View.GONE);
        } else {
            layoutInventoryEmpty.setVisibility(View.GONE);
            rvInventoryProducts.setVisibility(View.VISIBLE);
        }
    }

    private void refreshInventory() {
        refreshCategoryChips();
        filterProducts();
    }

    // ==========================================
    // TAB 3: STOCK MOVEMENTS / HISTORY
    // ==========================================

    private void setupHistoryTab() {
        chipGroupHistoryType = viewHistory.findViewById(R.id.chip_group_history_type);
        tvHistoryCountLabel = viewHistory.findViewById(R.id.tv_history_count_label);
        rvHistoryTransactions = viewHistory.findViewById(R.id.rv_history_transactions);
        layoutHistoryEmpty = viewHistory.findViewById(R.id.layout_history_empty);

        rvHistoryTransactions.setLayoutManager(new LinearLayoutManager(this));
        fullHistoryAdapter = new TransactionAdapter(this, new ArrayList<>());
        rvHistoryTransactions.setAdapter(fullHistoryAdapter);

        chipGroupHistoryType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_history_all) {
                    selectedHistoryType = "ALL";
                } else if (id == R.id.chip_history_in) {
                    selectedHistoryType = "IN";
                } else if (id == R.id.chip_history_out) {
                    selectedHistoryType = "OUT";
                }
                refreshHistory();
            }
        });
    }

    private void refreshHistory() {
        List<StockTransaction> filtered = new ArrayList<>();
        for (StockTransaction t : masterTransactionList) {
            if ("ALL".equalsIgnoreCase(selectedHistoryType) || (t.getType() != null && t.getType().equalsIgnoreCase(selectedHistoryType))) {
                filtered.add(t);
            }
        }

        fullHistoryAdapter.updateList(filtered);
        tvHistoryCountLabel.setText("Total " + filtered.size() + " Cloud Transactions Logged");

        if (filtered.isEmpty()) {
            layoutHistoryEmpty.setVisibility(View.VISIBLE);
            rvHistoryTransactions.setVisibility(View.GONE);
        } else {
            layoutHistoryEmpty.setVisibility(View.GONE);
            rvHistoryTransactions.setVisibility(View.VISIBLE);
        }
    }

    // ==========================================
    // TAB 4: ADMIN & USER MANAGEMENT
    // ==========================================

    private void setupAdminTab() {
        tvAdminProfileInitial = viewAdmin.findViewById(R.id.tv_admin_profile_initial);
        tvAdminProfileName = viewAdmin.findViewById(R.id.tv_admin_profile_name);
        tvAdminProfileUsername = viewAdmin.findViewById(R.id.tv_admin_profile_username);
        tvAdminProfileRole = viewAdmin.findViewById(R.id.tv_admin_profile_role);
        layoutUserManagementSection = viewAdmin.findViewById(R.id.layout_user_management_section);
        tvStaffRestrictedNotice = viewAdmin.findViewById(R.id.tv_staff_restricted_notice);
        btnAdminAddUser = viewAdmin.findViewById(R.id.btn_admin_add_user);
        rvAdminUsers = viewAdmin.findViewById(R.id.rv_admin_users);
        rowChangePassword = viewAdmin.findViewById(R.id.row_change_password);
        dividerResetData = viewAdmin.findViewById(R.id.divider_reset_data);
        rowResetData = viewAdmin.findViewById(R.id.row_reset_data);
        rowLogout = viewAdmin.findViewById(R.id.row_logout);

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this, new ArrayList<>(), sessionManager.getUserId(), new UserAdapter.OnUserActionListener() {
            @Override
            public void onEditUser(User user) {
                showAddEditUserDialog(user);
            }

            @Override
            public void onDeleteUser(User user) {
                confirmDeleteUser(user);
            }

            @Override
            public void onViewActivity(User user) {
                showUserActivityDialog(user);
            }
        });
        rvAdminUsers.setAdapter(userAdapter);

        btnAdminAddUser.setOnClickListener(v -> showAddEditUserDialog(null));
        rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        if (rowResetData != null) {
            rowResetData.setOnClickListener(v -> {
                if (!sessionManager.isAdmin()) {
                    Toast.makeText(this, "Access denied: Only administrators can reset system data", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmResetAllData();
            });
        }
        rowLogout.setOnClickListener(v -> confirmLogout());
    }

    private void refreshAdmin() {
        tvAdminProfileName.setText(sessionManager.getFullName());
        tvAdminProfileUsername.setText("@" + sessionManager.getUsername() + " (" + sessionManager.getEmail() + ")");
        tvAdminProfileRole.setText(sessionManager.getUserRole());
        String initial = !sessionManager.getFullName().isEmpty()
                ? sessionManager.getFullName().substring(0, 1).toUpperCase()
                : "U";
        tvAdminProfileInitial.setText(initial);

        boolean isAdmin = sessionManager.isAdmin();
        if (isAdmin) {
            layoutUserManagementSection.setVisibility(View.VISIBLE);
            tvStaffRestrictedNotice.setVisibility(View.GONE);
            if (rowResetData != null) rowResetData.setVisibility(View.VISIBLE);
            if (dividerResetData != null) dividerResetData.setVisibility(View.VISIBLE);
            userAdapter.updateList(masterUserList);
        } else {
            layoutUserManagementSection.setVisibility(View.GONE);
            tvStaffRestrictedNotice.setVisibility(View.VISIBLE);
            if (rowResetData != null) rowResetData.setVisibility(View.GONE);
            if (dividerResetData != null) dividerResetData.setVisibility(View.GONE);
        }
    }

    // ==========================================
    // DIALOGS: ADD / EDIT PRODUCT
    // ==========================================

    private void showAddEditProductDialog(Product productToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_product, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_product_title);
        View layoutImageContainer = dialogView.findViewById(R.id.layout_dialog_image_container);
        ImageView ivPreview = dialogView.findViewById(R.id.iv_dialog_product_preview);
        ImageView ivPlaceholder = dialogView.findViewById(R.id.iv_dialog_product_icon);
        MaterialButton btnTakePhoto = dialogView.findViewById(R.id.btn_dialog_take_photo);
        MaterialButton btnSelectImage = dialogView.findViewById(R.id.btn_dialog_select_image);
        TextView tvImageStatus = dialogView.findViewById(R.id.tv_dialog_image_status);
        TextView btnRemoveImage = dialogView.findViewById(R.id.btn_dialog_remove_image);
        TextInputEditText etSku = dialogView.findViewById(R.id.et_dialog_sku);
        MaterialButton btnGenSku = dialogView.findViewById(R.id.btn_dialog_gen_sku);
        MaterialButton btnScanSku = dialogView.findViewById(R.id.btn_dialog_scan_sku);
        TextInputEditText etName = dialogView.findViewById(R.id.et_dialog_name);
        TextView btnToggleNewCategory = dialogView.findViewById(R.id.btn_dialog_toggle_new_category);
        TextInputLayout tilCategory = dialogView.findViewById(R.id.til_dialog_category);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actv_dialog_category);
        TextInputLayout tilCustomCategory = dialogView.findViewById(R.id.til_dialog_custom_category);
        TextInputEditText etCustomCategory = dialogView.findViewById(R.id.et_dialog_custom_category);
        TextInputEditText etBuyPrice = dialogView.findViewById(R.id.et_dialog_buy_price);
        TextInputEditText etSellPrice = dialogView.findViewById(R.id.et_dialog_sell_price);
        TextInputLayout tilQuantity = dialogView.findViewById(R.id.til_dialog_quantity);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.et_dialog_quantity);
        TextInputEditText etMinStock = dialogView.findViewById(R.id.et_dialog_min_stock);
        TextInputEditText etDesc = dialogView.findViewById(R.id.et_dialog_description);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        currentDialogImageView = ivPreview;
        currentDialogPlaceholder = ivPlaceholder;
        currentDialogImageStatus = tvImageStatus;
        currentDialogRemoveImage = btnRemoveImage;
        currentDialogBase64 = null;

        final String SPECIAL_ADD_NEW = "➕ + Add New Category...";

        // Collect existing unique categories
        Set<String> catSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        catSet.add("Electronics");
        catSet.add("Accessories");
        catSet.add("Office Supplies");
        catSet.add("Storage");
        catSet.add("Food & Beverages");
        catSet.add("Clothing");
        catSet.add("Hardware & Tools");
        catSet.add("General");

        for (Product p : masterProductList) {
            if (p.getCategory() != null && !p.getCategory().trim().isEmpty()) {
                catSet.add(p.getCategory().trim());
            }
        }
        if (productToEdit != null && productToEdit.getCategory() != null && !productToEdit.getCategory().trim().isEmpty()) {
            catSet.add(productToEdit.getCategory().trim());
        }

        List<String> categoryList = new ArrayList<>(catSet);
        categoryList.add(SPECIAL_ADD_NEW);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown_category, categoryList);
        actvCategory.setAdapter(categoryAdapter);
        actvCategory.setOnClickListener(v -> actvCategory.showDropDown());

        // Toggle button listener
        btnToggleNewCategory.setOnClickListener(v -> {
            boolean currentlyCustom = tilCustomCategory.getVisibility() == View.VISIBLE;
            if (currentlyCustom) {
                tilCustomCategory.setVisibility(View.GONE);
                etCustomCategory.setText("");
                btnToggleNewCategory.setText("➕ Add New Category");
                tilCustomCategory.setError(null);
            } else {
                tilCustomCategory.setVisibility(View.VISIBLE);
                btnToggleNewCategory.setText("✕ Choose Existing");
                etCustomCategory.requestFocus();
                tilCategory.setError(null);
            }
        });

        // Dropdown selection listener
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            tilCategory.setError(null);
            if (SPECIAL_ADD_NEW.equals(selected)) {
                actvCategory.setText("", false);
                tilCustomCategory.setVisibility(View.VISIBLE);
                btnToggleNewCategory.setText("✕ Choose Existing");
                etCustomCategory.requestFocus();
            } else {
                tilCustomCategory.setVisibility(View.GONE);
                etCustomCategory.setText("");
                btnToggleNewCategory.setText("➕ Add New Category");
            }
        });

        etCustomCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilCustomCategory.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        boolean isEdit = productToEdit != null;
        if (isEdit) {
            tvTitle.setText("Edit Product Details");
            etSku.setText(productToEdit.getSku());
            etName.setText(productToEdit.getName());
            if (productToEdit.getCategory() != null && !productToEdit.getCategory().trim().isEmpty()) {
                actvCategory.setText(productToEdit.getCategory().trim(), false);
            }
            etBuyPrice.setText(String.valueOf(productToEdit.getBuyPrice()));
            etSellPrice.setText(String.valueOf(productToEdit.getSellPrice()));
            etQuantity.setText(String.valueOf(productToEdit.getQuantity()));
            tilQuantity.setHelperText("Current stock: " + productToEdit.getQuantity());
            etMinStock.setText(String.valueOf(productToEdit.getMinStockLevel()));
            etDesc.setText(productToEdit.getDescription());

            if (productToEdit.getImageBase64() != null && !productToEdit.getImageBase64().isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(productToEdit.getImageBase64(), Base64.DEFAULT);
                    Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivPreview.setImageBitmap(b);
                    ivPlaceholder.setVisibility(View.GONE);
                    currentDialogBase64 = productToEdit.getImageBase64();
                    if (tvImageStatus != null) {
                        tvImageStatus.setText("Existing photo (" + (bytes.length / 1024) + " KB)");
                    }
                    if (btnRemoveImage != null) {
                        btnRemoveImage.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            tvTitle.setText("Add New Product");
            etSku.setText("SKU-" + (1000 + new Random().nextInt(9000)));
            if (!categoryList.isEmpty() && !categoryList.get(0).equals(SPECIAL_ADD_NEW)) {
                actvCategory.setText(categoryList.get(0), false);
            }
        }

        btnGenSku.setOnClickListener(v -> etSku.setText("SKU-" + (1000 + new Random().nextInt(9000))));
        if (btnScanSku != null) {
            btnScanSku.setOnClickListener(v -> launchBarcodeScanner(etSku));
        }

        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndLaunch());
        }
        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> launchGalleryPicker());
        }
        if (layoutImageContainer != null) {
            layoutImageContainer.setOnClickListener(v -> showImageSourceOptionsDialog());
        }
        if (btnRemoveImage != null) {
            btnRemoveImage.setOnClickListener(v -> clearDialogImage());
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        currentProductDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (currentProductDialog == dialog) {
                currentProductDialog = null;
            }
            currentDialogImageView = null;
            currentDialogPlaceholder = null;
            currentDialogImageStatus = null;
            currentDialogRemoveImage = null;
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String sku = etSku.getText() != null ? etSku.getText().toString().trim().toUpperCase() : "";
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            
            boolean isCustomCategory = tilCustomCategory.getVisibility() == View.VISIBLE;
            String category;
            if (isCustomCategory) {
                category = etCustomCategory.getText() != null ? etCustomCategory.getText().toString().trim() : "";
            } else {
                category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
            }

            String buyStr = etBuyPrice.getText() != null ? etBuyPrice.getText().toString().trim() : "";
            String sellStr = etSellPrice.getText() != null ? etSellPrice.getText().toString().trim() : "";
            String qtyStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String minStockStr = etMinStock.getText() != null ? etMinStock.getText().toString().trim() : "5";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

            if (sku.isEmpty()) {
                etSku.setError("SKU code is required");
                etSku.requestFocus();
                return;
            }

            // SKU uniqueness validation
            for (Product p : masterProductList) {
                if (p.getSku() != null && p.getSku().equalsIgnoreCase(sku)) {
                    if (!isEdit || (productToEdit != null && !p.getId().equals(productToEdit.getId()))) {
                        etSku.setError("SKU '" + sku + "' already exists for (" + p.getName() + ")");
                        etSku.requestFocus();
                        return;
                    }
                }
            }

            if (name.isEmpty()) {
                etName.setError("Product name is required");
                etName.requestFocus();
                return;
            }

            if (category.isEmpty() || SPECIAL_ADD_NEW.equals(category)) {
                if (isCustomCategory) {
                    tilCustomCategory.setError("Please enter the new category name");
                    etCustomCategory.requestFocus();
                } else {
                    tilCategory.setError("Please select or enter a category");
                    actvCategory.requestFocus();
                }
                return;
            }

            if (buyStr.isEmpty()) {
                etBuyPrice.setError("Cost price is required");
                etBuyPrice.requestFocus();
                return;
            }

            double buyPrice;
            try {
                buyPrice = Double.parseDouble(buyStr);
                if (buyPrice < 0) {
                    etBuyPrice.setError("Cost price cannot be negative");
                    etBuyPrice.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etBuyPrice.setError("Enter a valid price");
                etBuyPrice.requestFocus();
                return;
            }

            if (sellStr.isEmpty()) {
                etSellPrice.setError("Selling price is required");
                etSellPrice.requestFocus();
                return;
            }

            double sellPrice;
            try {
                sellPrice = Double.parseDouble(sellStr);
                if (sellPrice < 0) {
                    etSellPrice.setError("Selling price cannot be negative");
                    etSellPrice.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etSellPrice.setError("Enter a valid price");
                etSellPrice.requestFocus();
                return;
            }

            if (qtyStr.isEmpty()) {
                etQuantity.setError("Quantity is required");
                etQuantity.requestFocus();
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(qtyStr);
                if (quantity < 0) {
                    etQuantity.setError("Quantity cannot be negative");
                    etQuantity.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etQuantity.setError("Enter a whole number");
                etQuantity.requestFocus();
                return;
            }

            int minStock = 5;
            if (!minStockStr.isEmpty()) {
                try {
                    minStock = Integer.parseInt(minStockStr);
                    if (minStock < 0) {
                        etMinStock.setError("Alert level cannot be negative");
                        etMinStock.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etMinStock.setError("Enter a whole number");
                    etMinStock.requestFocus();
                    return;
                }
            }

            btnSave.setEnabled(false);

            if (isEdit) {
                productToEdit.setSku(sku);
                productToEdit.setName(name);
                productToEdit.setCategory(category);
                productToEdit.setBuyPrice(buyPrice);
                productToEdit.setSellPrice(sellPrice);
                productToEdit.setQuantity(quantity);
                productToEdit.setMinStockLevel(minStock);
                productToEdit.setDescription(desc);
                productToEdit.setImageBase64(currentDialogBase64);
                firebaseHelper.updateProduct(productToEdit, new FirebaseHelper.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Product updated in Firebase Cloud", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Update failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Product newProduct = new Product(sku, name, category, buyPrice, sellPrice, quantity, minStock, desc, currentDialogBase64);
                String currentUsername = sessionManager.getUsername();
                firebaseHelper.addProduct(newProduct, currentUsername, new FirebaseHelper.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Product added to Firebase Cloud", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Failed to add product: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    // ==========================================
    // DIALOGS: STOCK IN / STOCK OUT ADJUSTMENT
    // ==========================================

    private void showProductSelectDialog(String type) {
        if (masterProductList.isEmpty()) {
            Toast.makeText(this, "No products available to adjust stock. Add a product first.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isIn = "IN".equalsIgnoreCase(type);

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bottom_sheet_select_product, null);
        sheet.setContentView(view);

        // Header views
        View layoutIconBg = view.findViewById(R.id.layout_sheet_select_icon_bg);
        ImageView ivIcon = view.findViewById(R.id.iv_sheet_select_icon);
        TextView tvTitle = view.findViewById(R.id.tv_sheet_select_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_sheet_select_subtitle);
        ImageButton btnClose = view.findViewById(R.id.btn_sheet_select_close);

        // Search views
        EditText etSearch = view.findViewById(R.id.et_sheet_select_search);
        ImageButton btnClearSearch = view.findViewById(R.id.btn_sheet_select_clear_search);
        TextView tvCount = view.findViewById(R.id.tv_sheet_select_count);

        // List views
        RecyclerView rvProducts = view.findViewById(R.id.rv_sheet_select_products);
        View layoutEmpty = view.findViewById(R.id.layout_sheet_select_empty);

        // Configure theme based on Stock In vs Stock Out
        if (isIn) {
            tvTitle.setText("Select Product for Stock In (+)");
            tvSubtitle.setText("Choose a product to receive new stock inventory");
            ivIcon.setImageResource(R.drawable.ic_stock_in);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_in_stock));
            layoutIconBg.setBackgroundResource(R.drawable.bg_circle_icon_green);
        } else {
            tvTitle.setText("Select Product for Stock Out (-)");
            tvSubtitle.setText("Choose a product to deduct sold or damaged inventory");
            ivIcon.setImageResource(R.drawable.ic_stock_out);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_low_stock));
            layoutIconBg.setBackgroundResource(R.drawable.bg_circle_icon_amber);
        }

        // Setup RecyclerView & Adapter
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        SelectProductAdapter adapter = new SelectProductAdapter(this, masterProductList, type, product -> {
            sheet.dismiss();
            showStockAdjustDialog(product, type);
        });
        rvProducts.setAdapter(adapter);

        tvCount.setText("Showing " + masterProductList.size() + " product" + (masterProductList.size() == 1 ? "" : "s"));

        // Real-time search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                int matchCount = adapter.filter(query);
                if (matchCount == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvProducts.setVisibility(View.GONE);
                    tvCount.setText("0 products found");
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvProducts.setVisibility(View.VISIBLE);
                    if (query.isEmpty()) {
                        tvCount.setText("Showing " + matchCount + " product" + (matchCount == 1 ? "" : "s"));
                    } else {
                        tvCount.setText("Found " + matchCount + " matching product" + (matchCount == 1 ? "" : "s"));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        btnClose.setOnClickListener(v -> sheet.dismiss());

        sheet.show();
    }

    private void showStockAdjustDialog(Product product, String defaultType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_stock_adjust, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_adjust_dialog_title);
        TextView tvName = dialogView.findViewById(R.id.tv_adjust_product_name);
        TextView tvSku = dialogView.findViewById(R.id.tv_adjust_product_sku);
        TextView tvCurrentStock = dialogView.findViewById(R.id.tv_adjust_current_stock);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_adjust_type);
        RadioButton rbIn = dialogView.findViewById(R.id.rb_stock_in);
        RadioButton rbOut = dialogView.findViewById(R.id.rb_stock_out);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.et_adjust_quantity);
        TextInputEditText etReason = dialogView.findViewById(R.id.et_adjust_reason);
        TextView tvCalcPreview = dialogView.findViewById(R.id.tv_adjust_calculation_preview);

        MaterialButton btnAdd1 = dialogView.findViewById(R.id.btn_qty_add_1);
        MaterialButton btnAdd5 = dialogView.findViewById(R.id.btn_qty_add_5);
        MaterialButton btnAdd10 = dialogView.findViewById(R.id.btn_qty_add_10);
        MaterialButton btnAdd25 = dialogView.findViewById(R.id.btn_qty_add_25);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_adjust_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_adjust_confirm);

        tvName.setText(product.getName());
        tvSku.setText("SKU: " + (product.getSku() != null && !product.getSku().isEmpty() ? product.getSku() : "N/A"));
        tvCurrentStock.setText("Current: " + product.getQuantity());

        if (product.isOutOfStock()) {
            tvCurrentStock.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.status_out_of_stock));
        } else if (product.isLowStock()) {
            tvCurrentStock.setBackgroundResource(R.drawable.bg_badge_low_stock);
            tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.status_low_stock));
        } else {
            tvCurrentStock.setBackgroundResource(R.drawable.bg_badge_in_stock);
            tvCurrentStock.setTextColor(ContextCompat.getColor(this, R.color.status_in_stock));
        }

        if ("OUT".equalsIgnoreCase(defaultType)) {
            rbOut.setChecked(true);
            if (tvDialogTitle != null) tvDialogTitle.setText("Stock Out (-)");
            btnConfirm.setText("Deduct Stock (-)");
            etReason.setText("Customer Order / Sale");
        } else {
            rbIn.setChecked(true);
            if (tvDialogTitle != null) tvDialogTitle.setText("Stock In (+)");
            btnConfirm.setText("Add Stock (+)");
            etReason.setText("Supplier Restock / Purchase");
        }

        Runnable updateCalc = () -> {
            boolean isIn = rbIn.isChecked();
            String qtyStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "0";
            int qty = 0;
            try {
                if (!qtyStr.isEmpty()) qty = Integer.parseInt(qtyStr);
            } catch (Exception ignored) {}

            int curr = product.getQuantity();
            int resulting = isIn ? (curr + qty) : (curr - qty);

            if (!isIn && resulting < 0) {
                tvCalcPreview.setText("⚠️ Warning: Resulting stock will be negative! (Max out: " + curr + ")");
                tvCalcPreview.setTextColor(ContextCompat.getColor(this, R.color.status_out_of_stock));
            } else {
                tvCalcPreview.setText("Result: Current " + curr + (isIn ? " + " : " - ") + qty + " = New Stock: " + resulting + " Units");
                tvCalcPreview.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
        };

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_stock_in) {
                if (tvDialogTitle != null) tvDialogTitle.setText("Stock In (+)");
                btnConfirm.setText("Add Stock (+)");
                etReason.setText("Supplier Restock / Purchase");
            } else {
                if (tvDialogTitle != null) tvDialogTitle.setText("Stock Out (-)");
                btnConfirm.setText("Deduct Stock (-)");
                etReason.setText("Customer Order / Sale");
            }
            updateCalc.run();
        });

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCalc.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener stepListener = v -> {
            int toAdd = 1;
            if (v.getId() == R.id.btn_qty_add_5) toAdd = 5;
            else if (v.getId() == R.id.btn_qty_add_10) toAdd = 10;
            else if (v.getId() == R.id.btn_qty_add_25) toAdd = 25;

            int currentInput = 0;
            String curStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            try {
                if (!curStr.isEmpty()) currentInput = Integer.parseInt(curStr);
            } catch (Exception ignored) {}

            etQuantity.setText(String.valueOf(currentInput + toAdd));
        };

        btnAdd1.setOnClickListener(stepListener);
        btnAdd5.setOnClickListener(stepListener);
        btnAdd10.setOnClickListener(stepListener);
        btnAdd25.setOnClickListener(stepListener);

        etQuantity.setText("1");
        updateCalc.run();

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String qtyStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            boolean isIn = rbIn.isChecked();

            if (qtyStr.isEmpty()) {
                etQuantity.setError("Please enter quantity");
                etQuantity.requestFocus();
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
                if (qty <= 0) {
                    etQuantity.setError("Quantity must be greater than 0");
                    etQuantity.requestFocus();
                    return;
                }
            } catch (Exception e) {
                etQuantity.setError("Please enter a valid whole number");
                etQuantity.requestFocus();
                return;
            }

            if (!isIn && product.getQuantity() < qty) {
                etQuantity.setError("Cannot deduct " + qty + ". Available stock is only " + product.getQuantity());
                etQuantity.requestFocus();
                Toast.makeText(this, "Cannot deduct " + qty + ". Available stock is only " + product.getQuantity(), Toast.LENGTH_LONG).show();
                return;
            }

            if (reason.isEmpty()) {
                etReason.setError("Please specify reason or reference");
                etReason.requestFocus();
                return;
            }

            btnConfirm.setEnabled(false);
            String type = isIn ? "IN" : "OUT";
            String user = sessionManager.getUsername();

            firebaseHelper.adjustStock(product, type, qty, reason, user, new FirebaseHelper.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Stock " + (isIn ? "added (+)" : "deducted (-)") + " to Cloud!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    // ==========================================
    // DIALOGS: PRODUCT DETAILS
    // ==========================================

    private void showProductDetailsDialog(Product p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_product_details, null);
        builder.setView(view);

        ImageView ivImg = view.findViewById(R.id.iv_detail_image);
        ImageView ivPlaceholder = view.findViewById(R.id.iv_detail_placeholder);
        TextView tvName = view.findViewById(R.id.tv_detail_name);
        TextView tvSku = view.findViewById(R.id.tv_detail_sku);
        TextView tvCat = view.findViewById(R.id.tv_detail_category);
        TextView tvStockBadge = view.findViewById(R.id.tv_detail_stock_badge);
        TextView tvBuyPrice = view.findViewById(R.id.tv_detail_buy_price);
        TextView tvSellPrice = view.findViewById(R.id.tv_detail_sell_price);
        TextView tvMargin = view.findViewById(R.id.tv_detail_profit_margin);
        TextView tvHoldingVal = view.findViewById(R.id.tv_detail_total_holding_value);
        TextView tvDesc = view.findViewById(R.id.tv_detail_desc);
        MaterialButton btnClose = view.findViewById(R.id.btn_detail_close);
        MaterialButton btnAdjust = view.findViewById(R.id.btn_detail_adjust);
        MaterialButton btnEdit = view.findViewById(R.id.btn_detail_edit);

        tvName.setText(p.getName());
        tvSku.setText("SKU: " + p.getSku());
        tvCat.setText(p.getCategory());
        tvBuyPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", p.getBuyPrice()));
        tvSellPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", p.getSellPrice()));
        tvMargin.setText(String.format(Locale.getDefault(), "+%.1f%%", p.getProfitMargin()));
        tvHoldingVal.setText(String.format(Locale.getDefault(), "Rs. %,.2f", p.getTotalRetailValue()));

        if (p.getDescription() != null && !p.getDescription().trim().isEmpty()) {
            tvDesc.setText(p.getDescription());
        } else {
            tvDesc.setText("No additional description provided.");
        }

        if (p.isOutOfStock()) {
            tvStockBadge.setText("Out of Stock (0)");
            tvStockBadge.setBackgroundResource(R.drawable.bg_badge_out_of_stock);
            tvStockBadge.setTextColor(ContextCompat.getColor(this, R.color.status_out_of_stock));
        } else if (p.isLowStock()) {
            tvStockBadge.setText("Low Stock: " + p.getQuantity());
            tvStockBadge.setBackgroundResource(R.drawable.bg_badge_low_stock);
            tvStockBadge.setTextColor(ContextCompat.getColor(this, R.color.status_low_stock));
        } else {
            tvStockBadge.setText("In Stock: " + p.getQuantity());
            tvStockBadge.setBackgroundResource(R.drawable.bg_badge_in_stock);
            tvStockBadge.setTextColor(ContextCompat.getColor(this, R.color.status_in_stock));
        }

        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ivImg.setImageBitmap(b);
                ivPlaceholder.setVisibility(View.GONE);
            } catch (Exception ignored) {}
        }

        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnAdjust.setOnClickListener(v -> {
            dialog.dismiss();
            showStockAdjustDialog(p, "IN");
        });

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showAddEditProductDialog(p);
        });

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    private void confirmDeleteProduct(Product product) {
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Only Administrators can delete products.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Product?")
                .setMessage("Are you sure you want to delete '" + product.getName() + "' from Cloud Database? This action will sync to all devices.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseHelper.deleteProduct(product.getId(), new FirebaseHelper.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Product deleted from Cloud", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, "Delete failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==========================================
    // DIALOGS: USER ACCOUNTS MANAGEMENT
    // ==========================================

    private void showAddEditUserDialog(User userToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_user, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tv_user_dialog_title);
        TextInputEditText etFullName = view.findViewById(R.id.et_dialog_user_fullname);
        TextInputEditText etUsername = view.findViewById(R.id.et_dialog_user_username);
        TextInputEditText etEmail = view.findViewById(R.id.et_dialog_user_email);
        TextInputLayout tilPassword = view.findViewById(R.id.til_dialog_user_password);
        TextInputEditText etPassword = view.findViewById(R.id.et_dialog_user_password);
        RadioGroup rgRole = view.findViewById(R.id.rg_dialog_user_role);
        RadioButton rbStaff = view.findViewById(R.id.rb_role_staff);
        RadioButton rbAdmin = view.findViewById(R.id.rb_role_admin);
        MaterialButton btnCancel = view.findViewById(R.id.btn_dialog_user_cancel);
        MaterialButton btnSave = view.findViewById(R.id.btn_dialog_user_save);

        boolean isEdit = userToEdit != null;
        if (isEdit) {
            tvTitle.setText("Edit User Account");
            etFullName.setText(userToEdit.getFullName());
            etUsername.setText(userToEdit.getUsername());
            etUsername.setEnabled(false);
            etEmail.setText(userToEdit.getEmail());
            tilPassword.setHint("New Password (Leave blank to keep current)");

            if (userToEdit.isAdmin()) {
                rbAdmin.setChecked(true);
            } else {
                rbStaff.setChecked(true);
            }
        } else {
            tvTitle.setText("Create New User Account");
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String role = rbAdmin.isChecked() ? "ADMIN" : "STAFF";

            if (fullName.isEmpty()) {
                etFullName.setError("Full name is required");
                etFullName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError("Email address is required");
                etEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address (e.g. name@domain.com)");
                etEmail.requestFocus();
                return;
            }

            // Duplicate Email check
            for (User u : masterUserList) {
                if (u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(email)) {
                    if (!isEdit || (userToEdit != null && !u.getId().equals(userToEdit.getId()))) {
                        etEmail.setError("Email already in use by @" + u.getUsername());
                        etEmail.requestFocus();
                        return;
                    }
                }
            }

            if (!isEdit) {
                if (username.isEmpty()) {
                    etUsername.setError("Username is required");
                    etUsername.requestFocus();
                    return;
                }
                if (username.length() < 3) {
                    etUsername.setError("Username must be at least 3 characters");
                    etUsername.requestFocus();
                    return;
                }
                if (username.contains(" ")) {
                    etUsername.setError("Username cannot contain spaces");
                    etUsername.requestFocus();
                    return;
                }

                // Duplicate Username check
                for (User u : masterUserList) {
                    if (u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(username)) {
                        etUsername.setError("Username @" + username + " is already taken");
                        etUsername.requestFocus();
                        return;
                    }
                }

                if (password.isEmpty()) {
                    etPassword.setError("Password is required");
                    etPassword.requestFocus();
                    return;
                }
                if (password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                    return;
                }
            } else {
                if (!password.isEmpty() && password.length() < 6) {
                    etPassword.setError("New password must be at least 6 characters");
                    etPassword.requestFocus();
                    return;
                }
            }

            btnSave.setEnabled(false);

            if (!isEdit) {
                User newUser = new User(username, email, password, fullName, role);
                firebaseHelper.addUser(newUser, new FirebaseHelper.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "User @" + username + " added to Cloud", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                userToEdit.setFullName(fullName);
                userToEdit.setEmail(email);
                userToEdit.setRole(role);
                if (!password.isEmpty()) {
                    userToEdit.setPassword(password);
                }
                firebaseHelper.updateUser(userToEdit, new FirebaseHelper.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "User updated in Cloud", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    private void confirmDeleteUser(User user) {
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Only administrators can delete accounts", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete User @" + user.getUsername() + "?")
                .setMessage("Are you sure you want to permanently delete account '" + user.getFullName() + "' from Cloud Database?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseHelper.deleteUser(user.getId(), new FirebaseHelper.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "User deleted from Cloud", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUserActivityDialog(User user) {
        if (user == null) return;
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Only administrators can view user activity audits", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bottom_sheet_user_activity, null);
        sheet.setContentView(view);

        TextView tvAvatar = view.findViewById(R.id.tv_sheet_user_avatar);
        TextView tvFullName = view.findViewById(R.id.tv_sheet_user_fullname);
        TextView tvRole = view.findViewById(R.id.tv_sheet_user_role);
        TextView tvUsername = view.findViewById(R.id.tv_sheet_user_username);
        ImageButton btnClose = view.findViewById(R.id.btn_sheet_user_close);

        TextView tvTotalOps = view.findViewById(R.id.tv_sheet_user_total_ops);
        TextView tvInUnits = view.findViewById(R.id.tv_sheet_user_in_units);
        TextView tvOutUnits = view.findViewById(R.id.tv_sheet_user_out_units);
        TextView tvSectionTitle = view.findViewById(R.id.tv_sheet_user_section_title);

        RecyclerView rvUserTransactions = view.findViewById(R.id.rv_sheet_user_transactions);
        View layoutEmpty = view.findViewById(R.id.layout_sheet_user_empty);

        String initial = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName().substring(0, 1).toUpperCase()
                : "U";
        tvAvatar.setText(initial);
        tvFullName.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        tvRole.setText(user.getRole() != null ? user.getRole() : "STAFF");

        if (user.isAdmin()) {
            tvRole.setBackgroundResource(R.drawable.bg_badge_in_stock);
            tvRole.setTextColor(ContextCompat.getColor(this, R.color.status_in_stock));
        } else {
            tvRole.setBackgroundResource(R.drawable.bg_badge_neutral);
            tvRole.setTextColor(ContextCompat.getColor(this, R.color.accent_blue));
        }

        String emailPart = (user.getEmail() != null && !user.getEmail().isEmpty()) ? " • " + user.getEmail() : "";
        tvUsername.setText("@" + user.getUsername() + emailPart);

        btnClose.setOnClickListener(v -> sheet.dismiss());

        // Filter transactions for this specific user
        List<StockTransaction> userTransactions = new ArrayList<>();
        int totalOps = 0;
        int totalInUnits = 0;
        int totalOutUnits = 0;

        String targetUsername = user.getUsername() != null ? user.getUsername().trim() : "";

        for (StockTransaction t : masterTransactionList) {
            String performedBy = t.getPerformedBy() != null ? t.getPerformedBy().trim() : "";
            if (targetUsername.equalsIgnoreCase(performedBy)) {
                userTransactions.add(t);
                totalOps++;
                if ("IN".equalsIgnoreCase(t.getType())) {
                    totalInUnits += Math.abs(t.getQuantity());
                } else if ("OUT".equalsIgnoreCase(t.getType())) {
                    totalOutUnits += Math.abs(t.getQuantity());
                }
            }
        }

        tvTotalOps.setText(String.valueOf(totalOps));
        tvInUnits.setText("+" + totalInUnits);
        tvOutUnits.setText("-" + totalOutUnits);
        tvSectionTitle.setText("Activity & Movement History (" + userTransactions.size() + ")");

        if (userTransactions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvUserTransactions.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvUserTransactions.setVisibility(View.VISIBLE);
            rvUserTransactions.setLayoutManager(new LinearLayoutManager(this));
            TransactionAdapter adapter = new TransactionAdapter(this, userTransactions);
            rvUserTransactions.setAdapter(adapter);
        }

        sheet.show();
    }

    // ==========================================
    // SYSTEM SETTINGS: CHANGE PASSWORD & RESET
    // ==========================================

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        TextInputEditText etCurrent = view.findViewById(R.id.et_pwd_current);
        TextInputEditText etNew = view.findViewById(R.id.et_pwd_new);
        TextInputEditText etConfirm = view.findViewById(R.id.et_pwd_confirm);
        MaterialButton btnCancel = view.findViewById(R.id.btn_pwd_cancel);
        MaterialButton btnSave = view.findViewById(R.id.btn_pwd_save);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String current = etCurrent.getText() != null ? etCurrent.getText().toString().trim() : "";
            String newPwd = etNew.getText() != null ? etNew.getText().toString().trim() : "";
            String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

            if (current.isEmpty()) {
                etCurrent.setError("Current password is required");
                etCurrent.requestFocus();
                return;
            }

            if (newPwd.isEmpty()) {
                etNew.setError("New password is required");
                etNew.requestFocus();
                return;
            }

            if (newPwd.length() < 6) {
                etNew.setError("New password must be at least 6 characters");
                etNew.requestFocus();
                return;
            }

            if (confirm.isEmpty()) {
                etConfirm.setError("Please confirm new password");
                etConfirm.requestFocus();
                return;
            }

            if (!newPwd.equals(confirm)) {
                etConfirm.setError("Passwords do not match");
                etConfirm.requestFocus();
                return;
            }

            if (newPwd.equals(current)) {
                etNew.setError("New password must be different from current password");
                etNew.requestFocus();
                return;
            }

            btnSave.setEnabled(false);
            firebaseHelper.changePassword(sessionManager.getUserId(), current, newPwd, new FirebaseHelper.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Password updated in Cloud!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    etCurrent.setError(message);
                }
            });
        });

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    private void confirmResetAllData() {
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Only administrators can reset system data", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Reset All System Data?")
                .setMessage("This will PERMANENTLY REMOVE all inventory products, stock transactions, and history from Cloud Database.\n\nOnly your Admin account will be preserved.\n\nNO DEMO DATA will be reloaded. Are you sure you want to completely clear everything?")
                .setPositiveButton("Reset & Delete All", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "Wiping system data...", Toast.LENGTH_SHORT).show();
                    firebaseHelper.resetAllData(sessionManager.getUserId(), new FirebaseHelper.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            masterProductList.clear();
                            masterTransactionList.clear();
                            refreshDashboard();
                            refreshInventory();
                            refreshHistory();
                            refreshAdmin();
                            Toast.makeText(MainActivity.this, "All products and transaction data removed! System reset complete.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, "Reset failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==========================================
    // BARCODE & QR SCANNER
    // ==========================================

    private void launchBarcodeScanner(TextInputEditText targetInput) {
        currentSkuInputForScan = targetInput;
        ScanOptions options = new ScanOptions();
        options.setPrompt("Align Barcode or QR Code inside the scanner frame");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        barcodeScannerLauncher.launch(options);
    }

    // ==========================================
    // NOTIFICATION BADGE & ALERTS DRAWER
    // ==========================================

    private void refreshNotificationBadge() {
        int alertCount = 0;
        for (Product p : masterProductList) {
            if (p.isLowStock() || p.isOutOfStock()) {
                alertCount++;
            }
        }
        if (tvNotificationBadge != null) {
            if (alertCount > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(alertCount > 99 ? "99+" : String.valueOf(alertCount));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }

    private List<Product> getLowAndOutOfStockProducts() {
        List<Product> alertList = new ArrayList<>();
        for (Product p : masterProductList) {
            if (p.isLowStock() || p.isOutOfStock()) {
                alertList.add(p);
            }
        }
        return alertList;
    }

    private void showLowStockAlertsSheet() {
        List<Product> allAlerts = getLowAndOutOfStockProducts();

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bottom_sheet_alerts, null);
        sheet.setContentView(view);

        TextView tvSubtitle = view.findViewById(R.id.tv_sheet_alerts_subtitle);
        ImageButton btnClose = view.findViewById(R.id.btn_sheet_alerts_close);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_sheet_alerts);
        RecyclerView rvAlerts = view.findViewById(R.id.rv_sheet_alerts);
        View layoutEmpty = view.findViewById(R.id.layout_sheet_alerts_empty);
        MaterialButton btnExport = view.findViewById(R.id.btn_sheet_export_reorder_pdf);

        if (allAlerts.isEmpty()) {
            tvSubtitle.setText("All inventory levels are optimal! 🎉");
            layoutEmpty.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
            btnExport.setEnabled(false);
        } else {
            tvSubtitle.setText(allAlerts.size() + " products require restocking");
            layoutEmpty.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
            btnExport.setEnabled(true);
        }

        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        AlertItemAdapter adapter = new AlertItemAdapter(this, allAlerts, product -> {
            sheet.dismiss();
            showStockAdjustDialog(product, "IN");
        });
        rvAlerts.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                List<Product> filtered = new ArrayList<>();
                if (id == R.id.chip_alert_all) {
                    filtered.addAll(allAlerts);
                } else if (id == R.id.chip_alert_out) {
                    for (Product p : allAlerts) {
                        if (p.isOutOfStock()) filtered.add(p);
                    }
                } else if (id == R.id.chip_alert_low) {
                    for (Product p : allAlerts) {
                        if (p.isLowStock()) filtered.add(p);
                    }
                }
                adapter.updateList(filtered);
                if (filtered.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvAlerts.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvAlerts.setVisibility(View.VISIBLE);
                }
            }
        });

        btnExport.setOnClickListener(v -> {
            File pdf = ReportGenerator.generateLowStockPdf(this, allAlerts, sessionManager.getFullName());
            if (pdf != null) {
                ReportGenerator.shareFile(this, pdf, "application/pdf", "Supplier Reorder Sheet");
            } else {
                Toast.makeText(this, "Failed to generate report", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> sheet.dismiss());

        sheet.show();
    }

    // ==========================================
    // EXPORT REPORTS
    // ==========================================

    private void showExportReportsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_export_reports, null);
        builder.setView(view);

        View cardInventory = view.findViewById(R.id.card_export_inventory_pdf);
        View cardLowStock = view.findViewById(R.id.card_export_low_stock_pdf);
        View cardCsv = view.findViewById(R.id.card_export_transactions_csv);
        MaterialButton btnCancel = view.findViewById(R.id.btn_export_dialog_cancel);

        AlertDialog dialog = builder.create();

        cardInventory.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Generating Inventory PDF...", Toast.LENGTH_SHORT).show();
            File file = ReportGenerator.generateStockInventoryPdf(this, masterProductList, sessionManager.getFullName());
            if (file != null) {
                ReportGenerator.shareFile(this, file, "application/pdf", "Stock Inventory Report");
            } else {
                Toast.makeText(this, "Error creating PDF report", Toast.LENGTH_SHORT).show();
            }
        });

        cardLowStock.setOnClickListener(v -> {
            dialog.dismiss();
            List<Product> lowStock = getLowAndOutOfStockProducts();
            if (lowStock.isEmpty()) {
                Toast.makeText(this, "No low-stock items to export!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Generating Reorder Sheet PDF...", Toast.LENGTH_SHORT).show();
            File file = ReportGenerator.generateLowStockPdf(this, lowStock, sessionManager.getFullName());
            if (file != null) {
                ReportGenerator.shareFile(this, file, "application/pdf", "Supplier Reorder Sheet");
            } else {
                Toast.makeText(this, "Error creating Reorder Sheet", Toast.LENGTH_SHORT).show();
            }
        });

        cardCsv.setOnClickListener(v -> {
            dialog.dismiss();
            if (masterTransactionList.isEmpty()) {
                Toast.makeText(this, "No transaction history to export!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Generating Movements CSV...", Toast.LENGTH_SHORT).show();
            File file = ReportGenerator.generateTransactionsCsv(this, masterTransactionList);
            if (file != null) {
                ReportGenerator.shareFile(this, file, "text/csv", "Stock Movements Audit Log");
            } else {
                Toast.makeText(this, "Error creating CSV log", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        applyDialogWindowStyles(dialog);
    }

    // ==========================================
    // VISUAL CHARTS & ANALYTICS
    // ==========================================

    private void updateDashboardCharts(double totalRetailVal) {
        if (chartStockValuation == null || layoutChartLegends == null) return;

        Map<String, Double> categoryValMap = new HashMap<>();
        for (Product p : masterProductList) {
            String cat = p.getCategory() != null && !p.getCategory().trim().isEmpty() ? p.getCategory().trim() : "General";
            categoryValMap.put(cat, categoryValMap.getOrDefault(cat, 0.0) + p.getTotalRetailValue());
        }

        int[] colors = {0xFF2563EB, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC4899, 0xFF06B6D4, 0xFF6366F1, 0xFF64748B};
        List<StockDonutChartView.ChartSlice> slices = new ArrayList<>();
        int colorIdx = 0;

        for (Map.Entry<String, Double> entry : categoryValMap.entrySet()) {
            if (entry.getValue() > 0) {
                int c = colors[colorIdx % colors.length];
                slices.add(new StockDonutChartView.ChartSlice(entry.getKey(), entry.getValue(), c));
                colorIdx++;
            }
        }

        chartStockValuation.setData(slices, "TOTAL VALUE", String.format(Locale.getDefault(), "Rs. %,.0f", totalRetailVal));

        // Update Legends container
        layoutChartLegends.removeAllViews();

        TextView title = new TextView(this);
        title.setText("Valuation by Category");
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setTextSize(12f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setPadding(0, 0, 0, 8);
        layoutChartLegends.addView(title);

        if (slices.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No valuation data available");
            emptyTv.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            emptyTv.setTextSize(11f);
            layoutChartLegends.addView(emptyTv);
        } else {
            int shown = 0;
            for (StockDonutChartView.ChartSlice slice : slices) {
                if (shown >= 4) break;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 3, 0, 3);

                View dot = new View(this);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(16, 16);
                dotParams.setMargins(0, 0, 10, 0);
                dot.setLayoutParams(dotParams);
                dot.setBackgroundColor(slice.color);

                TextView tvLabel = new TextView(this);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvLabel.setLayoutParams(labelParams);
                tvLabel.setText(slice.label);
                tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvLabel.setTextSize(11f);
                tvLabel.setSingleLine(true);

                TextView tvVal = new TextView(this);
                tvVal.setText(String.format(Locale.getDefault(), "%.0f%%", slice.percentage));
                tvVal.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                tvVal.setTextSize(11f);
                tvVal.setTypeface(tvVal.getTypeface(), Typeface.BOLD);

                row.addView(dot);
                row.addView(tvLabel);
                row.addView(tvVal);
                layoutChartLegends.addView(row);
                shown++;
            }
        }

        // Compute total In and Out volume from transactions
        int totalInUnits = 0;
        int totalOutUnits = 0;
        for (StockTransaction t : masterTransactionList) {
            if ("IN".equalsIgnoreCase(t.getType())) {
                totalInUnits += t.getQuantity();
            } else if ("OUT".equalsIgnoreCase(t.getType())) {
                totalOutUnits += t.getQuantity();
            }
        }

        if (tvChartStockInFlow != null) {
            tvChartStockInFlow.setText("📥 Total In: " + totalInUnits + " units");
        }
        if (tvChartStockOutFlow != null) {
            tvChartStockOutFlow.setText("📤 Total Out: " + totalOutUnits + " units");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentCameraPhotoPath != null) {
            outState.putString("current_camera_photo_path", currentCameraPhotoPath);
        }
        if (currentDialogBase64 != null) {
            outState.putString("current_dialog_base64", currentDialogBase64);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            NetworkUtil.unregisterNetworkListener(this, networkCallback);
        }
    }
}
