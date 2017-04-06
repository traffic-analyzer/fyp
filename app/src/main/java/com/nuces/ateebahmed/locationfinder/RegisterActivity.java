package com.nuces.ateebahmed.locationfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
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

    private static final String TAG = "RegisterActivity";
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etUser, etPassword, etName, etEmail;
    private AppCompatButton btnRegister;
    private DatabaseReference dbUsersRef, conRef;
    private ValueEventListener usernameListener, connectionListener;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;
    private UserSession session;
    private boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        isConnected = false;

        usernameListener = usernameAvailable();

        userAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (isConnected && firebaseAuth.getCurrentUser() != null) {
                    addValuesInDatabase(firebaseAuth.getCurrentUser());
                    saveNewUserSession(firebaseAuth.getCurrentUser());
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "No Internet connection available",
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        session = new UserSession(getApplicationContext());

        tilEmail = (TextInputLayout) findViewById(R.id.tilEmail);
        tilPassword = (TextInputLayout) findViewById(R.id.tilPassword);
        etName = (TextInputEditText) findViewById(R.id.etName);
        etUser = (TextInputEditText) findViewById(R.id.etUser);
        etEmail = (TextInputEditText) findViewById(R.id.etEmail);
        etPassword = (TextInputEditText) findViewById(R.id.etPassword);
        btnRegister = (AppCompatButton) findViewById(R.id.btnRegister);
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
                addListenerforUsername();
            }
        });

        setEmailHelperText();
        setPasswordHelperText();
        etName.addTextChangedListener(fieldsEmpty);
//        etPassword.addTextChangedListener(fieldsEmpty);
        etUser.addTextChangedListener(fieldsEmpty);
//        etEmail.addTextChangedListener(fieldsEmpty);
    }

    @Override
    protected void onStart() {
        super.onStart();
        addConnectionListener();
        getInstances();
        userAuth.addAuthStateListener(userAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListenerforUsername();
        userAuth.removeAuthStateListener(userAuthListener);
        removeConnectionListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        addConnectionListener();
        getInstances();
        userAuth.addAuthStateListener(userAuthListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeListenerforUsername();
        userAuth.removeAuthStateListener(userAuthListener);
        removeConnectionListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void addListenerforUsername() {
        if (isConnected)
            dbUsersRef.orderByChild("username").equalTo(etUser.getText().toString().trim())
                    .addListenerForSingleValueEvent(usernameListener);
        else Toast.makeText(getApplicationContext(), "No internet connection available",
                Toast.LENGTH_LONG).show();
    }

    private void removeListenerforUsername() {
        dbUsersRef.orderByChild("username").equalTo(etUser.getText().toString().trim())
                .removeEventListener(usernameListener);
    }

    private void setEmailHelperText() {
        tilEmail.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tilEmail.setError(getString(R.string.etEmail));
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tilEmail.setError(getString(R.string.etEmail));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkFields();
            }
        });
    }

    private void setPasswordHelperText() {
        tilPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0) {
                    tilPassword.setError("atleast 8 characters long");
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() < 8) {
                    tilPassword.setError("atleast 8 characters long");
                } else tilPassword.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private ValueEventListener usernameAvailable() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!existInDatabase(dataSnapshot))
                    registerUserWithFirebase();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getCode() + ": " + databaseError.getMessage());
            }
        };
    }

    private void registerUserWithFirebase() {
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
                                }
                            }
                        });
    }

    private void saveNewUserSession(FirebaseUser user) {
        if (user != null) {
            session.createSession(etUser.getText().toString().trim(), user.getUid(),
                    etEmail.getText().toString().trim());
        }
    }

    private void getInstances() {
        if (userAuth == null && dbUsersRef == null) {
            DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
            dbUsersRef = dbRootRef.child("users");
            userAuth = FirebaseAuth.getInstance();
        }
    }

    private void startMapsActivtity() {
        Intent maps = new Intent(this, MapsActivity.class);
        startActivity(maps);
        finish();
    }

    private ValueEventListener checkConnectivity() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isConnected = dataSnapshot.getValue(Boolean.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    private void addConnectionListener() {
        if (connectionListener == null)
            connectionListener = checkConnectivity();
        if (conRef == null)
            conRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        conRef.addValueEventListener(connectionListener);
    }

    private void removeConnectionListener() {
        if (connectionListener != null) {
            conRef.removeEventListener(connectionListener);
            connectionListener = null;
        }
    }
}
