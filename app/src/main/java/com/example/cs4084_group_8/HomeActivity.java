package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {
    private TextView tvHomeTitle;
    private TextView tvHomeSubtitle;
    private MaterialButton btnRouteLog;
    private MaterialButton btnSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvHomeTitle = findViewById(R.id.tvHomeTitle);
        tvHomeSubtitle = findViewById(R.id.tvHomeSubtitle);
        btnRouteLog = findViewById(R.id.btnRouteLog);
        btnSignOut = findViewById(R.id.btnSignOut);

        bindCurrentUser();
        btnRouteLog.setOnClickListener(view -> startActivity(new Intent(this, RouteLogActivity.class)));
        btnSignOut.setOnClickListener(view -> signOut());
    }

    private void bindCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvHomeTitle.setText(R.string.home_guest_title);
            tvHomeSubtitle.setText(R.string.home_guest_subtitle);
            return;
        }

        String displayName = currentUser.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = currentUser.getEmail();
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = getString(R.string.home_member_fallback_name);
        }

        tvHomeTitle.setText(getString(R.string.home_title_format, displayName));
        tvHomeSubtitle.setText(R.string.home_subtitle);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
