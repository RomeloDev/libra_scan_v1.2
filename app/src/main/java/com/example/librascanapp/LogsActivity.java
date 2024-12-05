package com.example.librascanapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {

    private TableLayout logsTable;
    private TextView logoutText;
    private TextView printBtn;
    private List<LogEntry> todayLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        fetchTodayLogs();
    }


    private void initializeViews() {
        logsTable = findViewById(R.id.logsTable);
        logoutText = findViewById(R.id.textView2);
        printBtn = findViewById(R.id.print);

        logoutText.setOnClickListener(v -> showLogoutDialog());
        printBtn.setOnClickListener(v -> handlePrintAction());
    }

    private void handlePrintAction() {
        if (!todayLogs.isEmpty()) {
            exportLogsToPDF();
        } else {
            showToast("No logs found for today");
        }
    }

    private void fetchTodayLogs() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

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
                        showToast("Error: " + error.getMessage());
                    }
                });
    }

    private void fetchStudentData(String studentId, DataSnapshot logSnapshot) {
        FirebaseDatabase.getInstance().getReference("Students").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            LogEntry logEntry = new LogEntry(
                                    snapshot.child("id").getValue(String.class),
                                    snapshot.child("name").getValue(String.class),
                                    snapshot.child("courseYr").getValue(String.class),
                                    snapshot.child("department").getValue(String.class),
                                    logSnapshot.child("timestamp").getValue(String.class),
                                    logSnapshot.child("purpose").getValue(String.class)
                            );
                            todayLogs.add(logEntry);
                            addLogToTable(logEntry);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Error: " + error.getMessage());
                    }
                });
    }

    private void addLogToTable(LogEntry logEntry) {
        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        tableRow.addView(createTextView(logEntry.getId()));
        tableRow.addView(createTextView(logEntry.getName()));
        tableRow.addView(createTextView(logEntry.getCourseYr()));
        tableRow.addView(createTextView(logEntry.getDepartment()));
        tableRow.addView(createTextView(logEntry.getDate()));
        tableRow.addView(createTextView(logEntry.getTime()));
        tableRow.addView(createTextView(logEntry.getPurpose()));

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
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void exportLogsToPDF() {
        String filePath = getExternalFilesDir(null) + "/LibraryLogs.pdf";

        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            document.add(new Paragraph("Library Logs").setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));

            Table table = new Table(7);
            String[] headers = {"ID", "Name", "Course & Year", "Department", "Date", "Time", "Purpose"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            for (LogEntry log : todayLogs) {
                table.addCell(log.getId());
                table.addCell(log.getName());
                table.addCell(log.getCourseYr());
                table.addCell(log.getDepartment());
                table.addCell(log.getDate());
                table.addCell(log.getTime());
                table.addCell(log.getPurpose());
            }

            document.add(table);
            document.close();
            showToast("PDF saved at: " + filePath);
        } catch (Exception e) {
            showToast("Error creating PDF: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCourseYr() {
            return courseYr;
        }

        public String getDepartment() {
            return department;
        }

        public String getDate() {
            return timestamp.split(" ")[0];
        }

        public String getTime() {
            return timestamp.split(" ")[1];
        }

        public String getPurpose() {
            return purpose;
        }
    }
}
