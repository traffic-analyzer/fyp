package com.nuces.ateebahmed.locationfinder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import models.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUser, etPassword, etName, etEmail;
    private Button btnRegister;
    private DatabaseReference dbUsersRef;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");

        userAuth = FirebaseAuth.getInstance();
        userAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                addValuesInDatabase(firebaseAuth.getCurrentUser());
            }
        };

        etName = (EditText) findViewById(R.id.etName);
        etUser = (EditText) findViewById(R.id.etUser);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setEnabled(false);

        TextWatcher fieldsEmpty = new TextWatcher() {
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
                createNewUser();
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
        userAuth.addAuthStateListener(userAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        userAuth.removeAuthStateListener(userAuthListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userAuth.addAuthStateListener(userAuthListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        userAuth.removeAuthStateListener(userAuthListener);
    }

    private void addValuesInDatabase(FirebaseUser fUser) {
        if (fUser != null) {
            User mUser = new User(etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(), etUser.getText().toString().trim());
            dbUsersRef.child(fUser.getUid()).setValue(mUser);
        }
    }

    private boolean existInDatabase(DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
            for (DataSnapshot ids : dataSnapshot.getChildren()) {
                if (ids.child("username").getValue().equals(etUser.getText().toString().trim())) {
                    Toast.makeText(RegisterActivity.this, "Username not available",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return false;
    }

    private void checkFields() {
        if (etEmail.getText().toString().trim().isEmpty() ||
                etUser.getText().toString().trim().isEmpty() ||
                etPassword.getText().toString().trim().isEmpty() ||
                etName.getText().toString().trim().isEmpty())
            btnRegister.setEnabled(false);
        else btnRegister.setEnabled(true);
    }

    private void createNewUser() {
        dbUsersRef.orderByChild("email").equalTo(etEmail.getText().toString().trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!existInDatabase(dataSnapshot)) {
                            userAuth.createUserWithEmailAndPassword(etEmail.getText().toString().trim(),
                                    etPassword.getText().toString().trim())
                                    .addOnCompleteListener(RegisterActivity.this,
                                            new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    Log.i("REGISTER", "onComplete: " + task.isSuccessful());
                                                    if (!task.isSuccessful()) {
                                                        Log.e("REGISTER", "signup failed");
                                                        Toast.makeText(RegisterActivity.this, "Signup failed",
                                                                Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Signup successful",
                                                                Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                }
                                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
