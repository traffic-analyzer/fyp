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
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView regLink;
    private EditText etUsername, etPassLogin;
    private Button btnLogin;
    private DatabaseReference dbRootRef, dbUsersRef;
    private TextWatcher fieldsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Intent register = new Intent(MainActivity.this, RegisterActivity.class);
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

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (existsInDatabase(dataSnapshot)) {
                            /*Intent tracker = new Intent(MainActivity.this, Tracker.class);
                            startActivity(tracker);*/
                            Intent maps = new Intent(MainActivity.this, MapsActivity.class);
                            startActivity(maps);
                            finish();
                        }
                        else Toast.makeText(MainActivity.this, "Invalid Username/Password",
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

    private boolean existsInDatabase(DataSnapshot snapshot) {
        boolean result[] = {false, false};
        if(snapshot.exists()) {
            for (DataSnapshot ids: snapshot.getChildren()) {
                for (DataSnapshot keys: ids.getChildren()) {
                    if (keys.getKey().equals("username"))
                        if (keys.getValue().equals(etUsername.getText().toString())) {
                            result[0] = true;
                        }
                    if (keys.getKey().equals("password"))
                        if (keys.getValue().equals(etPassLogin.getText().toString())) {
                            result[1] = true;
                        }
                }
            }
            return result[0] && result[1];
        }
        return false;
    }

    private void checkFields() {
        if(etUsername.getText().toString().isEmpty() || etPassLogin.getText().toString().isEmpty())
            btnLogin.setEnabled(false);
        else btnLogin.setEnabled(true);
    }
}