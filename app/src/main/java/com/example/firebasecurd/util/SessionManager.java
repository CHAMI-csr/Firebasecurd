package com.example.firebasecurd.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.firebasecurd.model.User;

public class SessionManager {
    private static final String PREF_NAME = "StockMasterFirebaseSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_USER_ROLE = "userRole";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail() != null ? user.getEmail() : "");
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_USER_ROLE, user.getRole());
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "user");
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, "User");
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "STAFF");
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getUserRole());
    }

    public static final String KEY_THEME_MODE = "theme_mode";
    public static final int THEME_SYSTEM = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int THEME_LIGHT = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

    public void setThemeMode(int mode) {
        editor.putInt(KEY_THEME_MODE, mode);
        editor.apply();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
    }

    public int getThemeMode() {
        return pref.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public boolean isDarkMode() {
        int mode = getThemeMode();
        if (mode == THEME_DARK) return true;
        if (mode == THEME_LIGHT) return false;
        int nightModeFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public void applySavedTheme() {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(getThemeMode());
    }

    public void logoutUser() {
        int theme = getThemeMode();
        editor.clear();
        editor.putInt(KEY_THEME_MODE, theme);
        editor.apply();
    }
}
