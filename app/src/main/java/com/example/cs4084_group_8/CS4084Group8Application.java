package com.example.cs4084_group_8;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class CS4084Group8Application extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
