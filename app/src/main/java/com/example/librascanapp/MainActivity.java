package com.example.librascanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText idField;
    private Button signinBtn;
    private DatabaseReference studentsRef;
    private String getId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idField= findViewById(R.id.studentID);
        signinBtn = findViewById(R.id.signin);
        studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        signinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getId = idField.getText().toString();
                NetworkUtils.handleSignIn(MainActivity.this);
                verifyStudentID(getId);
            }
        });
    }

    private void verifyStudentID(String studentId){
        if (TextUtils.isEmpty(studentId)){
            Toast.makeText(this, "Please Enter your Student ID", Toast.LENGTH_SHORT).show();
            return;
        }else {
            studentsRef.orderByChild("id").equalTo(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                    String name = studentSnapshot.child("name").getValue(String.class);
                                    Toast.makeText(MainActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                                }

                                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                intent.putExtra("StudentId", getId);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "Invalid student ID", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("LoginActivity", "Database error: " + error.getMessage());
                        }
                    });
        }
    }
}