package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RootLoginActivity extends AppCompatActivity {
    public static final String EXTRA_ROOT_USERNAME = "extra_root_username";

    private static final String ROOT_USERNAME = "root";
    private static final int MIN_ACCESS_KEY_LENGTH = 6;

    private TextInputLayout tilRootUsername;
    private TextInputLayout tilRootAccessKey;
    private TextInputEditText etRootUsername;
    private TextInputEditText etRootAccessKey;
    private MaterialButton btnRootAccess;
    private TextView tvRootBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_login);

        tilRootUsername = findViewById(R.id.tilRootUsername);
        tilRootAccessKey = findViewById(R.id.tilRootAccessKey);
        etRootUsername = findViewById(R.id.etRootUsername);
        etRootAccessKey = findViewById(R.id.etRootAccessKey);
        btnRootAccess = findViewById(R.id.btnRootAccess);
        tvRootBack = findViewById(R.id.tvRootBack);

        btnRootAccess.setOnClickListener(view -> attemptRootLogin());
        tvRootBack.setOnClickListener(view -> finish());
    }

    private void attemptRootLogin() {
        tilRootUsername.setError(null);
        tilRootAccessKey.setError(null);

        String username = valueOf(etRootUsername);
        String accessKey = valueOf(etRootAccessKey);
        boolean hasError = false;

        if (TextUtils.isEmpty(username)) {
            tilRootUsername.setError(getString(R.string.root_login_username_required));
            hasError = true;
        } else if (!ROOT_USERNAME.equalsIgnoreCase(username)) {
            // Temporary local check until root auth is wired to a backend role system.
            tilRootUsername.setError(getString(R.string.root_login_username_invalid));
            hasError = true;
        }

        if (TextUtils.isEmpty(accessKey)) {
            tilRootAccessKey.setError(getString(R.string.root_login_access_key_required));
            hasError = true;
        } else if (accessKey.length() < MIN_ACCESS_KEY_LENGTH) {
            tilRootAccessKey.setError(getString(R.string.root_login_access_key_short));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        Intent intent = new Intent(this, RootDashboardActivity.class);
        intent.putExtra(EXTRA_ROOT_USERNAME, username.trim());
        startActivity(intent);
        finish();
    }

    private String valueOf(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }
}
