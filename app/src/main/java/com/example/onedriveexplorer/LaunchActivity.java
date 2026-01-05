package com.example.onedriveexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        Button btnOneDrive = findViewById(R.id.btn_onedrive);
        Button btnLocalStorage = findViewById(R.id.btn_local_storage);

        btnOneDrive.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnLocalStorage.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchActivity.this, LocalFilesActivity.class);
            startActivity(intent);
        });
    }
}
