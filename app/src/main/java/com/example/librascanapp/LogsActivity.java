package com.example.librascanapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;

public class LogsActivity extends AppCompatActivity {

    private static final String TAG = "LogsActivity";
    private TableLayout logsTable;
    private TextView logoutText;
    private TextView printBtn;
    private List<LogEntry> todayLogs = new ArrayList<>();
    private static final String[] TABLE_HEADERS = {"ID", "Name", "Course & Year", "Department", "Date", "Time", "Purpose"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        initializeViews();
        setupWindowInsets();

        String userID = getIntent().getStringExtra("userID");
        configurePrintButtonVisibility(userID);

        logoutText.setOnClickListener(v -> showLogoutDialog());
        printBtn.setOnClickListener(v -> handlePrintAction());

        fetchTodayLogs();
    }

    private void initializeViews() {
        logsTable = findViewById(R.id.logsTable);
        logoutText = findViewById(R.id.textView2);
        printBtn = findViewById(R.id.print);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void configurePrintButtonVisibility(String userID) {
        if ("1413914".equals(userID)) {
            printBtn.setVisibility(View.VISIBLE);
        } else {
            printBtn.setVisibility(View.GONE);
        }
    }

    private void handlePrintAction() {
        if (!todayLogs.isEmpty()) {
            exportLogsToPDF();
        } else {
            showToast("No logs found for today");
        }
    }

    private void fetchTodayLogs() {
        String today = getFormattedDate();

        FirebaseDatabase.getInstance().getReference("Logs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            todayLogs.clear();
                            for (DataSnapshot logEntry : snapshot.getChildren()) {
                                for (DataSnapshot dateSnapshot : logEntry.getChildren()) {
                                    String timestamp = dateSnapshot.child("timestamp").getValue(String.class);
                                    if (timestamp != null && timestamp.startsWith(today)) {
                                        fetchStudentData(logEntry.getKey(), dateSnapshot);
                                    }
                                }
                            }
                        } else {
                            showToast("No logs available");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Error fetching logs: " + error.getMessage());
                    }
                });
    }

    private void fetchStudentData(String studentId, DataSnapshot logSnapshot) {
        FirebaseDatabase.getInstance().getReference("Students").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            LogEntry logEntry = createLogEntry(snapshot, logSnapshot);
                            todayLogs.add(logEntry);
                            addLogToTable(logEntry);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Error fetching student data: " + error.getMessage());
                    }
                });
    }

    private LogEntry createLogEntry(DataSnapshot studentSnapshot, DataSnapshot logSnapshot) {
        return new LogEntry(
                safeGetString(studentSnapshot, "id"),
                safeGetString(studentSnapshot, "name"),
                safeGetString(studentSnapshot, "courseYr"),
                safeGetString(studentSnapshot, "department"),
                safeGetString(logSnapshot, "timestamp"),
                safeGetString(logSnapshot, "purpose")
        );
    }

    private void addLogToTable(LogEntry logEntry) {
        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        for (String value : logEntry.getAllFields()) {
            tableRow.addView(createTextView(value));
        }

        logsTable.addView(tableRow);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logoutAndRedirect())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logoutAndRedirect() {
        clearUserSession();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    private void exportLogsToPDF() {
        String filePath = createFilePath();

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument)) {

            document.add(new Paragraph("Library Logs").setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(createPDFTable());
            showToast("PDF saved at: " + filePath);

        } catch (Exception e) {
            Log.e(TAG, "Error creating PDF", e);
            showToast("Error creating PDF: " + e.getMessage());
        }
    }

    private String createFilePath() {
        File dir = getExternalFilesDir(null);
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return dir + "/LibraryLogs of " + LocalDate.now() + ".pdf";
    }

    private Table createPDFTable() {
        Table table = new Table(TABLE_HEADERS.length);
        for (String header : TABLE_HEADERS) {
            table.addCell(new Cell().add(new Paragraph(header)));
        }
        for (LogEntry log : todayLogs) {
            for (String value : log.getAllFields()) {
                table.addCell(new Cell().add(new Paragraph(value)));
            }
        }
        return table;
    }

    private String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String safeGetString(DataSnapshot snapshot, String key) {
        return snapshot.child(key).getValue(String.class) != null ? snapshot.child(key).getValue(String.class) : "N/A";
    }

    private static class LogEntry {
        private final String id, name, courseYr, department, timestamp, purpose;

        LogEntry(String id, String name, String courseYr, String department, String timestamp, String purpose) {
            this.id = id;
            this.name = name;
            this.courseYr = courseYr;
            this.department = department;
            this.timestamp = timestamp;
            this.purpose = purpose;
        }

        public String[] getAllFields() {
            return new String[]{
                    id,
                    name,
                    courseYr,
                    department,
                    getDate(),
                    getTime(),
                    purpose
            };
        }

        public String getDate() {
            return timestamp.contains(" ") ? timestamp.split(" ")[0] : "N/A";
        }

        public String getTime() {
            return timestamp.contains(" ") ? timestamp.split(" ")[1] : "N/A";
        }
    }
}
