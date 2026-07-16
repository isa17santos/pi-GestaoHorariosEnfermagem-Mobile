package com.pi.gestaohorariosenfermagemmobile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.res.Configuration;

/**
 * Base Activity that all activities in the application should extend.
 * Provides common functionality such as:
 * - Window insets handling for keyboard and system bars (API 30+)
 * - Language change handling through updateUIStrings() and updateLanguageButton()
 * - Configuration change management
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Apply window insets listener to handle keyboard and system bars correctly on Android API 30+.
     * This method adjusts the bottom padding of the root view to prevent the keyboard from covering
     * input fields and ensures proper spacing for system navigation bars.
     * 
     * Call this method in onCreate() after setContentView() for activities that have input fields.
     * 
     * @param rootView The root view of the activity (typically findViewById(android.R.id.content))
     */
    protected void applyWindowInsets(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Get the height of the IME (Input Method Editor - keyboard)
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            int imeHeight = imeInsets.bottom;
            
            // Get the height of system bars (navigation bar, status bar)
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int systemBarsHeight = systemBarsInsets.bottom;
            
            // Apply padding to ensure content is not covered by keyboard or system bars
            // Use the maximum of both to handle cases where either is visible
            v.setPadding(0, 0, 0, Math.max(imeHeight, systemBarsHeight));
            
            return insets;
        });
    }

    /**
     * Update all UI strings when language changes.
     * Each activity must implement this to update its specific UI elements.
     */
    protected abstract void updateUIStrings();

    /**
     * Update language button state when language changes.
     * Override this if your activity has a language switcher.
     */
    protected void updateLanguageButton() {
        boolean isEn = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("en");
        TextView tvFlag = findViewById(R.id.nav_tv_lang_flag);
        TextView tvLabel = findViewById(R.id.nav_tv_lang_label);
        if (tvFlag != null) tvFlag.setText(isEn ? "pt" : "en");
        if (tvLabel != null) tvLabel.setText(isEn ? "Português" : "English");
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        updateUIStrings();
        updateLanguageButton();
    }
}
