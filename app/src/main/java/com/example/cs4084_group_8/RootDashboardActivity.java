package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class RootDashboardActivity extends AppCompatActivity {
    private TextView tvRootGreeting;
    private MaterialButton btnRootExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_dashboard);

        tvRootGreeting = findViewById(R.id.tvRootGreeting);
        btnRootExit = findViewById(R.id.btnRootExit);

        String rootUsername = getIntent().getStringExtra(RootLoginActivity.EXTRA_ROOT_USERNAME);
        if (rootUsername == null || rootUsername.trim().isEmpty()) {
            rootUsername = getString(R.string.root_default_username);
        }

        tvRootGreeting.setText(getString(R.string.root_dashboard_greeting_format, rootUsername));
        btnRootExit.setOnClickListener(view -> returnToSignIn());
    }

    private void returnToSignIn() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
