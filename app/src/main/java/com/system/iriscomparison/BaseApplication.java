package com.system.iriscomparison;

import android.app.Application;

import com.xuexiang.xui.XUI;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XUI.init(this); //初始化UI框架
        XUI.initFontStyle("OPPOSans-M.ttf");
    }
}
