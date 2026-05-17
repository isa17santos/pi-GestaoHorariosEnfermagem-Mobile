package com.pi.gestaohorariosenfermagemmobile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.res.Configuration;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected abstract void updateUIStrings();

    protected void updateLanguageButton() {}

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
