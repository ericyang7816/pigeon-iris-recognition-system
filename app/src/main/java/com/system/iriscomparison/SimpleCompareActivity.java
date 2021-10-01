package com.system.iriscomparison;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xuexiang.xui.XUI;

public class SimpleCompareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XUI.initTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_compare);
    }
}