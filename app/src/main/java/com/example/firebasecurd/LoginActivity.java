package com.example.firebasecurd;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.firebasecurd.firebase.FirebaseHelper;
import com.example.firebasecurd.model.User;
import com.example.firebasecurd.util.NetworkUtil;
import com.example.firebasecurd.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsernameOrEmail, etPassword;
    private MaterialButton btnLogin;

    // Alert & Offline Views inside Card
    private View layoutOfflineBanner;
    private View layoutErrorAlert;
    private TextView tvLoginErrorTitle;
    private TextView tvLoginErrorMessage;
    private ImageButton btnDismissLoginError;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Configure edge-to-edge with dark status bar and light icons
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        try {
            androidx.core.view.WindowInsetsControllerCompat controller = 
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
            }
        } catch (Exception ignored) {}

        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.checkAndSeedDefaultData();

        // Handle Window Insets dynamically to avoid status bar & keyboard overlap
        View header = findViewById(R.id.layout_login_header);
        android.widget.ScrollView rootScroll = findViewById(R.id.login_root_scroll);
        if (header != null && rootScroll != null) {
            final int initialHeaderPaddingTop = header.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(rootScroll, (v, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

                header.setPadding(
                        header.getPaddingLeft(),
                        initialHeaderPaddingTop + systemBars.top,
                        header.getPaddingRight(),
                        header.getPaddingBottom()
                );

                int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
                rootScroll.setPadding(0, 0, 0, bottomPadding);

                if (ime.bottom > 0) {
                    rootScroll.postDelayed(() -> {
                        View focused = getCurrentFocus();
                        if (focused != null) {
                            int[] location = new int[2];
                            focused.getLocationInWindow(location);
                            rootScroll.smoothScrollBy(0, location[1] - 250);
                        } else {
                            rootScroll.smoothScrollTo(0, rootScroll.getBottom());
                        }
                    }, 100);
                }

                return windowInsets;
            });
            ViewCompat.requestApplyInsets(rootScroll);
        }

        etUsernameOrEmail = findViewById(R.id.et_username_or_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        // Alert views
        layoutOfflineBanner = findViewById(R.id.layout_login_offline_banner);
        layoutErrorAlert = findViewById(R.id.layout_login_error_alert);
        tvLoginErrorTitle = findViewById(R.id.tv_login_error_title);
        tvLoginErrorMessage = findViewById(R.id.tv_login_error_message);
        btnDismissLoginError = findViewById(R.id.btn_dismiss_login_error);

        if (btnDismissLoginError != null) {
            btnDismissLoginError.setOnClickListener(v -> hideError());
        }

        // Auto-hide error alert when user begins editing inputs
        TextWatcher autoClearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etUsernameOrEmail.addTextChangedListener(autoClearErrorWatcher);
        etPassword.addTextChangedListener(autoClearErrorWatcher);

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && rootScroll != null) {
                rootScroll.postDelayed(() -> rootScroll.smoothScrollTo(0, rootScroll.getBottom()), 150);
            }
        });
        etPassword.setOnClickListener(v -> {
            if (rootScroll != null) {
                rootScroll.postDelayed(() -> rootScroll.smoothScrollTo(0, rootScroll.getBottom()), 150);
            }
        });
        etUsernameOrEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && rootScroll != null && header != null) {
                rootScroll.postDelayed(() -> rootScroll.smoothScrollTo(0, (int) (header.getHeight() * 0.4f)), 150);
            }
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        // Realtime network monitoring
        updateNetworkUI(NetworkUtil.isNetworkAvailable(this));
        networkCallback = NetworkUtil.registerNetworkListener(this, this::updateNetworkUI);
    }

    private void updateNetworkUI(boolean isOnline) {
        runOnUiThread(() -> {
            if (layoutOfflineBanner != null) {
                if (isOnline) {
                    layoutOfflineBanner.setVisibility(View.GONE);
                } else {
                    layoutOfflineBanner.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showError(String title, String message) {
        if (tvLoginErrorTitle != null && title != null) {
            tvLoginErrorTitle.setText(title);
        }
        if (tvLoginErrorMessage != null && message != null) {
            tvLoginErrorMessage.setText(message);
        }
        if (layoutErrorAlert != null) {
            layoutErrorAlert.setVisibility(View.VISIBLE);
            layoutErrorAlert.setAlpha(0f);
            layoutErrorAlert.animate().alpha(1f).setDuration(220).start();
        }
    }

    private void hideError() {
        if (layoutErrorAlert != null && layoutErrorAlert.getVisibility() == View.VISIBLE) {
            layoutErrorAlert.setVisibility(View.GONE);
        }
    }

    private void attemptLogin() {
        String input = etUsernameOrEmail.getText() != null ? etUsernameOrEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (input.isEmpty()) {
            showError("Missing Username or Email", "Please enter your registered Email address or Username.");
            etUsernameOrEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Missing Password", "Please enter your account password to sign in.");
            etPassword.requestFocus();
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            showError("Offline Mode", "Device is currently offline. Please connect to Wi-Fi or Mobile Data to sign in.");
            return;
        }

        hideError();
        btnLogin.setEnabled(false);
        btnLogin.setText("Verifying...");

        firebaseHelper.loginWithUsernameOrEmail(input, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Sign In to System  ➔");
                sessionManager.createLoginSession(user);
                Toast.makeText(LoginActivity.this, "Welcome, " + user.getFullName() + " (" + user.getRole() + ")!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Sign In to System  ➔");
                showError("Authentication Failed", message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            NetworkUtil.unregisterNetworkListener(this, networkCallback);
        }
    }
}
