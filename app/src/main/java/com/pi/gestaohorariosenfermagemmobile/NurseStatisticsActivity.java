package com.pi.gestaohorariosenfermagemmobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NurseStatisticsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_statistics);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}