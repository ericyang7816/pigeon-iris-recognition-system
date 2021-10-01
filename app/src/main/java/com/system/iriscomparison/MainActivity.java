package com.system.iriscomparison;

import static com.xuexiang.xui.XUI.getContext;

import androidx.appcompat.app.AppCompatActivity;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.DensityUtils;
import com.xuexiang.xui.widget.actionbar.TitleBar;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xui.widget.layout.XUILinearLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private int mShadowElevationDp = 14;
    private float mShadowAlpha = 0.25f;
    private int mRadius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XUI.initTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TitleBar mTitleBar = findViewById(R.id.TitleBar);
        mTitleBar.disableLeftView();

        mRadius = DensityUtils.dp2px(getContext(), 15);
        XUILinearLayout selectLayout1 = findViewById(R.id.layout_select_1);
        selectLayout1.setRadiusAndShadow(mRadius,
                DensityUtils.dp2px(getContext(), mShadowElevationDp),
                mShadowAlpha);
        selectLayout1.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, SimpleCompareActivity.class));
        });
        XUILinearLayout selectLayout2 = findViewById(R.id.layout_select_2);
        selectLayout2.setRadiusAndShadow(mRadius,
                DensityUtils.dp2px(getContext(), mShadowElevationDp),
                mShadowAlpha);
        selectLayout2.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, SimpleCompareActivity.class));
        });


    }
}