package com.example.cs4084_group_8;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

public class RouteLogActivity extends AppCompatActivity {
    private TextInputLayout tilRouteName;
    private TextInputLayout tilRouteGrade;
    private TextInputLayout tilRouteAttempts;
    private TextInputLayout tilRouteStatus;
    private TextInputLayout tilRouteNotes;

    private TextInputEditText etRouteName;
    private TextInputEditText etRouteGrade;
    private TextInputEditText etRouteAttempts;
    private AutoCompleteTextView actvRouteStatus;
    private TextInputEditText etRouteNotes;
    private MaterialButton btnSaveRouteEntry;

    private TextView tvTotalRoutesValue;
    private TextView tvTotalAttemptsValue;
    private TextView tvAverageAttemptsValue;
    private TextView tvRouteHistoryEmpty;
    private RecyclerView rvRouteHistory;

    private RouteLogStore routeLogStore;
    private RouteLogAdapter routeLogAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_log);

        bindViews();
        configureToolbar();
        configureSendStatusDropdown();
        configureRouteHistoryList();

        routeLogStore = new RouteLogStore(this);
        btnSaveRouteEntry.setOnClickListener(view -> saveRouteEntry());
        loadRouteHistory();
    }

    private void bindViews() {
        tilRouteName = findViewById(R.id.tilRouteName);
        tilRouteGrade = findViewById(R.id.tilRouteGrade);
        tilRouteAttempts = findViewById(R.id.tilRouteAttempts);
        tilRouteStatus = findViewById(R.id.tilRouteStatus);
        tilRouteNotes = findViewById(R.id.tilRouteNotes);

        etRouteName = findViewById(R.id.etRouteName);
        etRouteGrade = findViewById(R.id.etRouteGrade);
        etRouteAttempts = findViewById(R.id.etRouteAttempts);
        actvRouteStatus = findViewById(R.id.actvRouteStatus);
        etRouteNotes = findViewById(R.id.etRouteNotes);
        btnSaveRouteEntry = findViewById(R.id.btnSaveRouteEntry);

        tvTotalRoutesValue = findViewById(R.id.tvTotalRoutesValue);
        tvTotalAttemptsValue = findViewById(R.id.tvTotalAttemptsValue);
        tvAverageAttemptsValue = findViewById(R.id.tvAverageAttemptsValue);
        tvRouteHistoryEmpty = findViewById(R.id.tvRouteHistoryEmpty);
        rvRouteHistory = findViewById(R.id.rvRouteHistory);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarRouteLog);
        toolbar.setNavigationIcon(R.drawable.ic_route_back);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void configureSendStatusDropdown() {
        String[] statuses = getResources().getStringArray(R.array.route_send_status_options);
        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses);
        actvRouteStatus.setAdapter(statusAdapter);
        if (statuses.length > 0) {
            actvRouteStatus.setText(statuses[0], false);
        }
    }

    private void configureRouteHistoryList() {
        rvRouteHistory.setLayoutManager(new LinearLayoutManager(this));
        routeLogAdapter = new RouteLogAdapter(getLayoutInflater());
        rvRouteHistory.setAdapter(routeLogAdapter);
    }

    private void saveRouteEntry() {
        clearInputErrors();

        String routeName = valueOf(etRouteName);
        String grade = valueOf(etRouteGrade);
        String attemptsRaw = valueOf(etRouteAttempts);
        String sendStatus = valueOf(actvRouteStatus);
        String notes = valueOf(etRouteNotes);

        boolean hasError = false;
        if (TextUtils.isEmpty(routeName)) {
            tilRouteName.setError(getString(R.string.route_log_route_name_required));
            hasError = true;
        }

        if (TextUtils.isEmpty(attemptsRaw)) {
            tilRouteAttempts.setError(getString(R.string.route_log_attempts_required));
            hasError = true;
        }

        int attempts = 0;
        if (!hasError) {
            try {
                attempts = Integer.parseInt(attemptsRaw);
            } catch (NumberFormatException ignored) {
                attempts = 0;
            }
            if (attempts <= 0) {
                tilRouteAttempts.setError(getString(R.string.route_log_attempts_invalid));
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        if (TextUtils.isEmpty(grade)) {
            grade = getString(R.string.route_log_grade_unknown);
        }
        if (TextUtils.isEmpty(sendStatus)) {
            sendStatus = getString(R.string.route_log_default_status);
        }

        RouteLogEntry entry = new RouteLogEntry(
                routeName,
                grade,
                attempts,
                sendStatus,
                notes,
                System.currentTimeMillis()
        );
        routeLogStore.addEntry(entry);

        clearQuickEntryFields();
        loadRouteHistory();
        Toast.makeText(this, R.string.route_log_saved_toast, Toast.LENGTH_SHORT).show();
    }

    private void clearInputErrors() {
        tilRouteName.setError(null);
        tilRouteGrade.setError(null);
        tilRouteAttempts.setError(null);
        tilRouteStatus.setError(null);
        tilRouteNotes.setError(null);
    }

    private void clearQuickEntryFields() {
        etRouteName.setText("");
        etRouteGrade.setText("");
        etRouteAttempts.setText("");
        etRouteNotes.setText("");
        actvRouteStatus.setText(getString(R.string.route_log_default_status), false);
    }

    private void loadRouteHistory() {
        List<RouteLogEntry> entries = routeLogStore.getAllEntries();
        routeLogAdapter.submitEntries(entries);
        updateSummary(entries);

        boolean hasEntries = !entries.isEmpty();
        rvRouteHistory.setVisibility(hasEntries ? View.VISIBLE : View.GONE);
        tvRouteHistoryEmpty.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
    }

    private void updateSummary(List<RouteLogEntry> entries) {
        int totalAttempts = 0;
        for (RouteLogEntry entry : entries) {
            totalAttempts += entry.getAttempts();
        }

        tvTotalRoutesValue.setText(getString(R.string.route_log_total_routes_value, entries.size()));
        tvTotalAttemptsValue.setText(getString(R.string.route_log_total_attempts_value, totalAttempts));

        float averageAttempts = entries.isEmpty() ? 0f : (float) totalAttempts / entries.size();
        String averageText = String.format(Locale.getDefault(), "%.1f", averageAttempts);
        tvAverageAttemptsValue.setText(getString(R.string.route_log_avg_attempts_value, averageText));
    }

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }
}
