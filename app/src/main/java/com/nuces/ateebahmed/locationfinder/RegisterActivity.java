package com.nuces.ateebahmed.locationfinder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import models.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUser, etPassword, etName, etEmail;
    private Button btnRegister;
    private DatabaseReference dbRootRef, dbUsersRef;
    private TextWatcher fieldsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");

        etName = (EditText) findViewById(R.id.etName);
        etUser = (EditText) findViewById(R.id.etUser);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setEnabled(false);

        fieldsEmpty = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkFields();
            }
        };

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!existInDatabase(dataSnapshot)) {
                            addValuesInDatabase();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("Firebase: ", databaseError.getMessage());
                    }
                });
            }
        });

        etName.addTextChangedListener(fieldsEmpty);
        etPassword.addTextChangedListener(fieldsEmpty);
        etUser.addTextChangedListener(fieldsEmpty);
        etEmail.addTextChangedListener(fieldsEmpty);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void addValuesInDatabase() {
        User user = new User(etName.getText().toString(), etEmail.getText().toString(),
                etUser.getText().toString(), etPassword.getText().toString());

        dbUsersRef.push().setValue(user);
        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
    }

    private boolean existInDatabase(DataSnapshot snapshot) {
        if(snapshot.exists()) {
            for (DataSnapshot ids: snapshot.getChildren()) {
                for (DataSnapshot keys: ids.getChildren()) {
                    if(keys.getKey().equals("email"))
                        if (keys.getValue().equals(etEmail.getText().toString())) {
                            Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    if (keys.getKey().equals("username"))
                        if (keys.getValue().equals(etUser.getText().toString())) {
                            Toast.makeText(this, "Username not available", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                }
            }
            return false;
        }
        return false;
    }

    private void checkFields() {
        if (etEmail.getText().toString().isEmpty() || etUser.getText().toString().isEmpty() ||
                etPassword.getText().toString().isEmpty() || etName.getText().toString().isEmpty())
            btnRegister.setEnabled(false);
        else btnRegister.setEnabled(true);
    }
}
