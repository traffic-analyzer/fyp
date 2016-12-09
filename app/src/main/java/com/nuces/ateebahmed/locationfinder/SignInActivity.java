package com.nuces.ateebahmed.locationfinder;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;

public class SignInActivity extends AppCompatActivity {

    private TextView regLink;
    private EditText etUsername, etPassLogin;
    private Button btnLogin;
    private DatabaseReference dbRootRef, dbUsersRef;
    private TextWatcher fieldsEmpty;
    private UserSession session;
    private String userKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        userKey = "";

        dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");

        regLink = (TextView) findViewById(R.id.regLink);
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

        etPassLogin.addTextChangedListener(fieldsEmpty);
        etUsername.addTextChangedListener(fieldsEmpty);

        session = new UserSession(getApplicationContext());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbUsersRef.orderByChild("username").equalTo(etUsername.getText().toString().trim())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (existsInDatabase(dataSnapshot)) {
                            Log.i("SIGNIN", userKey);
                            session.createSession(etUsername.getText().toString().trim(), userKey);
                            startMapsActivity();
                        } else Toast.makeText(SignInActivity.this, "Invalid Username/Password",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("FIREBASE", databaseError.getMessage());
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private boolean existsInDatabase(DataSnapshot dataSnapshot) {
        boolean result[] = {false, false};
                if (dataSnapshot.exists())
                    for (DataSnapshot ids: dataSnapshot.getChildren()) {
                        if (ids.child("username").getValue().toString()
                                .equals(etUsername.getText().toString().trim()))
                            result[0] = true;
                        if (ids.child("password").getValue().toString()
                                .equals(etPassLogin.getText().toString().trim()))
                            result[1] = true;
                        if (result[0] && result[1])
                            userKey = ids.getKey();
                        Log.i("SIGNIN", "username: " + result[0]);
                        Log.i("SIGNIN", "password: " + result[1]);
                    }
            return result[0] && result[1];
    }

    private void checkFields() {
        if(etUsername.getText().toString().trim().isEmpty() ||
                etPassLogin.getText().toString().trim().isEmpty())
            btnLogin.setEnabled(false);
        else btnLogin.setEnabled(true);
    }

    private void startMapsActivity() {
        Intent maps = new Intent(SignInActivity.this, MapsActivity.class);
        startActivity(maps);
        finish();
    }
}