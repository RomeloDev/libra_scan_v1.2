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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

public class LogsActivity extends AppCompatActivity {

    private TableLayout logsTable;
    private TextView logoutText;
    ArrayList<DataSnapshot> todayLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        // Set up edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        logsTable = findViewById(R.id.logsTable);
        logoutText = findViewById(R.id.textView2);

        logoutText.setOnClickListener(v -> {
            showLogoutDialog();
        });

        fetchTodayLogs();
    }

    private void fetchTodayLogs() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        FirebaseDatabase.getInstance().getReference("Logs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            todayLogs = new ArrayList<>();
                            for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                                for (DataSnapshot dateSnapshot : logSnapshot.getChildren()) {
                                    String timestamp = dateSnapshot.child("timestamp").getValue(String.class);

                                    if (timestamp != null && timestamp.startsWith(today)) {
                                        todayLogs.add(dateSnapshot);
                                    }
                                }
                            }

                            if (!todayLogs.isEmpty()) {
                                displayLogs(todayLogs);
                            } else {
                                Toast.makeText(LogsActivity.this, "No logs found for today", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LogsActivity.this, "No logs available", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LogsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayLogs(ArrayList<DataSnapshot> logs) {
        for (DataSnapshot logSnapshot : logs) {
            String studentId = logSnapshot.getRef().getParent().getKey();
            String purpose = logSnapshot.child("purpose").getValue(String.class);
            String timestamp = logSnapshot.child("timestamp").getValue(String.class);

            if (studentId != null && timestamp != null && purpose != null) {
                String[] dateTime = timestamp.split(" ");
                String date = dateTime[0];
                String time = dateTime[1];

                fetchStudentData(studentId, date, time, purpose);
            }
        }
    }

    private void fetchStudentData(String studentId, String date, String time, String purpose) {
        FirebaseDatabase.getInstance().getReference("Students").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String id = snapshot.child("id").getValue(String.class);
                            String name = snapshot.child("name").getValue(String.class);
                            String courseYr = snapshot.child("courseYr").getValue(String.class);
                            String department = snapshot.child("department").getValue(String.class);

                            if (id != null && name != null && courseYr != null && department != null) {
                                addTableRow(id, name, courseYr, department, date, time, purpose);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LogsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle logout logic here
                        logoutAndRedirect();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Close the dialog
                    }
                })
                .show();
    }

    private void logoutAndRedirect() {
        // Clear user session or preferences if needed
        // For example, clear SharedPreferences:
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Redirect to login activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Close current activity
    }

    private void addTableRow(String id, String name, String courseYr, String department, String date, String time, String purpose) {
        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        tableRow.addView(createTextView(id));
        tableRow.addView(createTextView(name));
        tableRow.addView(createTextView(courseYr));
        tableRow.addView(createTextView(department));
        tableRow.addView(createTextView(date));
        tableRow.addView(createTextView(time));
        tableRow.addView(createTextView(purpose));

        logsTable.addView(tableRow);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }



}
