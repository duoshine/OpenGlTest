package com.duoshine.opengltest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initView();
    }

    private void initView() {
        findViewById(R.id.preview).setOnClickListener(this);
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = null;

            ArrayList<String> strings = new ArrayList<>();
            strings.add(Manifest.permission.CAMERA);
            strings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            strings.add(Manifest.permission.RECORD_AUDIO);

            for (int i = 0; i < strings.size(); i++) {
                if (ActivityCompat.checkSelfPermission(this, strings.get(i)) != PackageManager.PERMISSION_GRANTED) {
                    if (permissions == null) {
                        permissions = new ArrayList<>();
                    }
                    permissions.add(strings.get(i));
                }
            }
            if (permissions != null && permissions.size() != 0) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), 11);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.preview:
                startActivity(new Intent(this,PreviewActivity.class));
                break;
        }
    }
}