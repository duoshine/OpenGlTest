package com.duoshine.opengltest;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.duoshine.opengltest.weiget.PreView;

/**
 * 预览
 */
public class PreviewActivity extends AppCompatActivity {

    private PreView preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        initView();
    }

    private void initView() {
        preview = findViewById(R.id.preview);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        preview.release();
    }
}