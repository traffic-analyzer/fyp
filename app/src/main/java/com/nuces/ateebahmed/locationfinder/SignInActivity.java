package com.nuces.ateebahmed.locationfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class SignInActivity extends AppCompatActivity {

    private EditText etUsername, etPassLogin;
    private Button btnLogin;
    private DatabaseReference dbUsersRef;
    private UserSession session;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;
    private User mUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");

        userAuth = FirebaseAuth.getInstance();
        userAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser fUser = firebaseAuth.getCurrentUser();
                if (fUser != null && mUser != null) {
                    session.createSession(mUser.getUsername(), fUser.getUid());
                    startMapsActivity();
                }
            }
        };

        TextView regLink = (TextView) findViewById(R.id.regLink);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassLogin = (EditText) findViewById(R.id.etPassLogin);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setEnabled(false);

        regLink.setMovementMethod(LinkMovementMethod.getInstance());
        Spannable span = (Spannable) regLink.getText();
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent register = new Intent(SignInActivity.this, RegisterActivity.class);
                startActivity(register);
            }
        };
        span.setSpan(clickableSpan, 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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

        etPassLogin.addTextChangedListener(fieldsEmpty);
        etUsername.addTextChangedListener(fieldsEmpty);

        session = new UserSession(getApplicationContext());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signinUser();
            }
        });

        mUser = new User();
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
    protected void onPause() {
        super.onPause();
        userAuth.removeAuthStateListener(userAuthListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userAuth.addAuthStateListener(userAuthListener);
    }

    private boolean existsInDatabase(DataSnapshot dataSnapshot) {
        boolean result = false;
                if (dataSnapshot.exists())
                    for (DataSnapshot ids: dataSnapshot.getChildren()) {
                        if (ids.child("username").getValue().toString()
                                .equals(etUsername.getText().toString().trim())) {
                            result = true;
                        }
                        if (result) {
                            mUser = ids.getValue(User.class);
                            break;
                        }
                    }
            return result;
    }

    private void checkFields() {
        if(etUsername.getText().toString().trim().isEmpty() ||
                etPassLogin.getText().toString().trim().isEmpty())
            btnLogin.setEnabled(false);
        else btnLogin.setEnabled(true);
    }

    private void signinUser() {
        dbUsersRef.orderByChild("username").equalTo(etUsername.getText().toString().trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (existsInDatabase(dataSnapshot)) {
                            userAuth.signInWithEmailAndPassword(mUser.getEmail(),
                                    etPassLogin.getText().toString().trim())
                                    .addOnCompleteListener(SignInActivity.this,
                                            new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    Log.i("SIGNIN", "onComplete: " + task.isSuccessful());
                                                    if (!task.isSuccessful()) {
                                                        Log.e("SIGNIN", "Signin failed");
                                                        Toast.makeText(SignInActivity.this,
                                                                "We could not find you here!",
                                                                Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Log.i("SIGNIN", "Signin successful");
                                                        Toast.makeText(SignInActivity.this,
                                                                "Great! Get started",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                        } else {
                            Log.e("SIGNIN", "Signin failed");
                            Toast.makeText(SignInActivity.this, "We could not find you here!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void startMapsActivity() {
        Intent maps = new Intent(SignInActivity.this, MapsActivity.class);
        startActivity(maps);
        finish();
    }
}