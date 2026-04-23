package com.pi.gestaohorariosenfermagemmobile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AbsencesActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absences);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
