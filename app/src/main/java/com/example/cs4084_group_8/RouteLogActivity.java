package com.example.cs4084_group_8;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private RouteLogAdapter routeLogAdapter;
    private ListenerRegistration routeHistoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_log);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        bindViews();
        configureToolbar();
        configureSendStatusDropdown();
        configureRouteHistoryList();

        btnSaveRouteEntry.setOnClickListener(view -> saveRouteEntry());
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        listenForRouteHistory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (routeHistoryListener != null) {
            routeHistoryListener.remove();
            routeHistoryListener = null;
        }
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
        routeLogAdapter = new RouteLogAdapter(getLayoutInflater(), this::confirmDeleteEntry);
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

        long attempts = 0;
        if (!hasError) {
            try {
                attempts = Long.parseLong(attemptsRaw);
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

        btnSaveRouteEntry.setEnabled(false);
        String finalGrade = grade;
        String finalSendStatus = sendStatus;
        long finalAttempts = attempts;
        firestore.collection(FirestoreCollections.USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String authorName = snapshot.getString("username");
                    if (TextUtils.isEmpty(authorName)) {
                        authorName = currentUser.getDisplayName();
                    }
                    if (TextUtils.isEmpty(authorName)) {
                        authorName = getString(R.string.route_log_default_author_name);
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("authorUid", currentUser.getUid());
                    data.put("authorName", authorName);
                    data.put("routeName", routeName);
                    data.put("grade", finalGrade);
                    data.put("attempts", finalAttempts);
                    data.put("sendStatus", finalSendStatus);
                    data.put("notes", notes);
                    data.put("loggedAt", FieldValue.serverTimestamp());

                    firestore.collection(FirestoreCollections.ROUTE_LOGS)
                            .add(data)
                            .addOnSuccessListener(documentReference -> {
                                btnSaveRouteEntry.setEnabled(true);
                                clearQuickEntryFields();
                                Toast.makeText(this, R.string.route_log_saved_toast, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                btnSaveRouteEntry.setEnabled(true);
                                Toast.makeText(this, getString(R.string.route_log_save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSaveRouteEntry.setEnabled(true);
                    Toast.makeText(this, getString(R.string.route_log_profile_load_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForRouteHistory() {
        if (routeHistoryListener != null) {
            routeHistoryListener.remove();
        }

        tvRouteHistoryEmpty.setVisibility(TextView.VISIBLE);
        tvRouteHistoryEmpty.setText(R.string.route_log_loading_history);
        rvRouteHistory.setVisibility(RecyclerView.GONE);

        routeHistoryListener = firestore.collection(FirestoreCollections.ROUTE_LOGS)
                .whereEqualTo("authorUid", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        rvRouteHistory.setVisibility(RecyclerView.GONE);
                        tvRouteHistoryEmpty.setVisibility(TextView.VISIBLE);
                        tvRouteHistoryEmpty.setText(getString(R.string.route_log_load_failed, error.getMessage()));
                        return;
                    }

                    List<RouteLogEntry> entries = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            RouteLogEntry entry = documentSnapshot.toObject(RouteLogEntry.class);
                            if (entry != null) {
                                entry.setId(documentSnapshot.getId());
                                entries.add(entry);
                            }
                        });
                    }

                    entries.sort(Comparator.comparing(
                            RouteLogEntry::getLoggedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed());

                    routeLogAdapter.submitEntries(entries);
                    updateSummary(entries);

                    boolean hasEntries = !entries.isEmpty();
                    rvRouteHistory.setVisibility(hasEntries ? RecyclerView.VISIBLE : RecyclerView.GONE);
                    tvRouteHistoryEmpty.setVisibility(hasEntries ? TextView.GONE : TextView.VISIBLE);
                    if (!hasEntries) {
                        tvRouteHistoryEmpty.setText(R.string.route_log_empty_history);
                    }
                });
    }

    private void updateSummary(List<RouteLogEntry> entries) {
        long totalAttempts = 0;
        for (RouteLogEntry entry : entries) {
            totalAttempts += entry.getAttempts();
        }

        tvTotalRoutesValue.setText(getString(R.string.route_log_total_routes_value, entries.size()));
        tvTotalAttemptsValue.setText(getString(R.string.route_log_total_attempts_value, totalAttempts));

        float averageAttempts = entries.isEmpty() ? 0f : (float) totalAttempts / entries.size();
        String averageText = String.format(Locale.getDefault(), "%.1f", averageAttempts);
        tvAverageAttemptsValue.setText(getString(R.string.route_log_avg_attempts_value, averageText));
    }

    private void confirmDeleteEntry(RouteLogEntry entry) {
        if (TextUtils.isEmpty(entry.getId())) {
            Toast.makeText(this, R.string.route_log_delete_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null || !TextUtils.equals(currentUser.getUid(), entry.getAuthorUid())) {
            Toast.makeText(this, R.string.route_log_delete_not_owner, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.route_log_delete_title)
                .setMessage(R.string.route_log_delete_message)
                .setPositiveButton(R.string.route_log_delete_confirm, (dialog, which) -> deleteEntry(entry.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteEntry(String entryId) {
        firestore.collection(FirestoreCollections.ROUTE_LOGS)
                .document(entryId)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.route_log_delete_success, Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.route_log_delete_failed, e.getMessage()), Toast.LENGTH_LONG).show()
                );
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

    private String valueOf(TextView view) {
        if (view.getText() == null) {
            return "";
        }
        return view.getText().toString().trim();
    }
}
