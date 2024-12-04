package com.example.librascanapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class HomeActivity extends AppCompatActivity {

    private Spinner purpose;
    private TextView idView, nameView, courseView, departmentView;
    private DatabaseReference studentsRef;
    private String selectedPurpose, studentID;
    private List<String> purposeOption;
    private boolean isFirstItemRemoved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        //set Default to HomePage
        bottomNavigationView.setSelectedItemId(R.id.home);

        idView = findViewById(R.id.idText);
        nameView = findViewById(R.id.nameText);
        courseView = findViewById(R.id.courseText);
        departmentView = findViewById(R.id.departmentText);

        studentID = getIntent().getStringExtra("StudentId");
        if (studentID == null || studentID.isEmpty()) {
            Toast.makeText(HomeActivity.this, "Student ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Students").child(studentID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            studentData data = snapshot.getValue(studentData.class);

                            if (data != null){
                                idView.setText(data.getId());
                                nameView.setText(data.getName());
                                courseView.setText(data.getCourseYr());
                                departmentView.setText(data.getDepartment());
                            }else {
                                Log.e("HomePage", "Model is null");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomeActivity.this, "Error: "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


        purpose = findViewById(R.id.spinner);

        purposeOption = new ArrayList<>();
        purposeOption.add("Select Here:");
        purposeOption.add("Study");
        purposeOption.add("Research");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, purposeOption);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        purpose.setAdapter(adapter);

        purpose.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isFirstItemRemoved) {
                    purposeOption.remove(0);
                    adapter.notifyDataSetChanged();
                    isFirstItemRemoved = true;
                    purpose.setSelection(1);
                }
                return false;
            }
        });

        purpose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPurpose = parent.getItemAtPosition(position).toString();
                Toast.makeText(HomeActivity.this, "Purpose of Visit: "+selectedPurpose, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(HomeActivity.this, "No purpose of visit selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle BottomNavigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        if (item.getItemId() == R.id.home) {
                            Toast.makeText(HomeActivity.this, "Home Selected", Toast.LENGTH_SHORT).show();
                            return true;
                        } else if (item.getItemId() == R.id.qr_scanner) {
                            if (selectedPurpose.equals("Select Here:")){
                                Toast.makeText(HomeActivity.this, "Please Select purpose of visit", Toast.LENGTH_SHORT).show();
                                return false;
                            }else {
                                initiateQRScan();  // Launch QR scanner
                                return true;
                            }
                        } else if (item.getItemId() == R.id.settings) {
                            Toast.makeText(HomeActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                            intent.putExtra("studentID", studentID);
                            startActivity(intent);
                            return true;
                        }
                        return false;
                    }
                });

    }

    // Method to initiate the QR code scan
    private void initiateQRScan() {
        new IntentIntegrator(this).initiateScan();
    }

    // Handle the result of the QR scan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String qrValue = result.getContents();

            if (qrValue.equals("Valid_QR")) {
                submitLogToFirebase(studentID, selectedPurpose);
            } else {
                Toast.makeText(this, "Invalid QR Code!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No QR Code Found!", Toast.LENGTH_SHORT).show();
        }
    }

    //method to prevent the duplication of logs based on current date
    private void submitLogToFirebase(String studentID, String purpose) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("Logs");

        // Get today's date in yyyy-MM-dd format
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Check if the student exists in the Students node
        studentsRef.child(studentID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Student exists, now check if a log already exists for today
                    checkDuplicateLog(logsRef, studentID, todayDate, purpose);
                } else {
                    // Student ID not found
                    Toast.makeText(HomeActivity.this, "Student not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkDuplicateLog(DatabaseReference logsRef, String studentID, String todayDate, String purpose) {
        // Query to check if today's log already exists for the student
        logsRef.child(studentID).child(todayDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Log already exists for today, show a message
                    Toast.makeText(HomeActivity.this, "You have already logged in today", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(HomeActivity.this, LogsActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // No log for today, proceed with log submission
                    submitNewLog(logsRef, studentID, todayDate, purpose);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitNewLog(DatabaseReference logsRef, String studentID, String todayDate, String purpose) {
        String logID = logsRef.child(studentID).child(todayDate).push().getKey();  // Generate a unique log ID
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Create a new log entry
        LogEntry logEntry = new LogEntry(logID, timestamp, purpose);

        // Submit the log to Firebase under Logs/{studentID}/{today's date}
        logsRef.child(studentID).child(todayDate).setValue(logEntry)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(HomeActivity.this, "Log submitted successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(HomeActivity.this, LogsActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, "Failed to submit log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}