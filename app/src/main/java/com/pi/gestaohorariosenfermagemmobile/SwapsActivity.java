package com.pi.gestaohorariosenfermagemmobile;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SwapsActivity extends BaseActivity {

    private TextView tvLangFlag, tvLangLabel;

    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swaps);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        //navbarManager = new NavbarManager(this);
    }

    //@Override
    //protected void onResume(){
        //super.onResume();
        //if(navbarManager != null) navbarManager.refreshUnreadCount();
    //}

    @Override
    protected void updateUIStrings() {
        // TO IMPLEMENT....

    }

    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }
}