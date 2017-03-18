package com.nuces.ateebahmed.locationfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import models.Message;

public class TextMessageActivity extends AppCompatActivity {

    private static final String TAG = "TextMessageActivity";
    private LocationComponentsSingleton instance;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver locationReceiver;
    private DatabaseReference dbMessagesRef;
    private UserSession session;
    private Location location;
    private RadioGroup rdGroup;
    private AppCompatRadioButton rdBlocked, rdSlowPace, rdNormal, rdSpeedy, rdNone;
    private AppCompatButton btnOptSend;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_message);

        rdBlocked = (AppCompatRadioButton) findViewById(R.id.rdBlocked);

        rdNormal = (AppCompatRadioButton) findViewById(R.id.rdNormal);

        rdSlowPace = (AppCompatRadioButton) findViewById(R.id.rdSlowPace);

        rdSpeedy = (AppCompatRadioButton) findViewById(R.id.rdSpeedy);

        rdNone = (AppCompatRadioButton) findViewById(R.id.rdNone);

        rdGroup = (RadioGroup) findViewById(R.id.rdGroup);

        btnOptSend = (AppCompatButton) findViewById(R.id.btnOptSend);
        btnOptSend.setOnClickListener(onButtonPressed());

        setInstance();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        locationReceiver = new LocationBroadcastReceiver();

        userAuth = FirebaseAuth.getInstance();

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");

    }

    @Override
    protected void onResume() {
        super.onResume();
        addAuthStateListener();
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(locationReceiver);
        removeAuthStateListener();
    }

    private View.OnClickListener onButtonPressed() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (rdGroup.getCheckedRadioButtonId()) {
                    case R.id.rdBlocked:
                        sendMessage((AppCompatRadioButton) view);
                        break;
                    case R.id.rdSlowPace:
                        sendMessage((AppCompatRadioButton) view);
                        break;
                    case R.id.rdNormal:
                        sendMessage((AppCompatRadioButton) view);
                        break;
                    case R.id.rdSpeedy:
                        sendMessage((AppCompatRadioButton) view);
                        break;
                    case R.id.rdNone:
                        sendMessage((AppCompatRadioButton) view);
                        break;
                    default:
                        try {
                            throw new Exception("Undefined button id");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };
    }

    private void showButtonToast(Button b) {
        Toast.makeText(this, b.getText().toString() + " was pressed", Toast.LENGTH_SHORT).show();
    }

    private void setInstance() {
        if (instance == null) {
            instance = LocationComponentsSingleton.getInstance(this);
        }
    }

    private void sendMessage(AppCompatRadioButton b) {
        if (location != null && session != null) {
            Message msg = new Message(b.getText().toString(), session.getSPUsername(),
                    location.getLongitude(), location.getLatitude(), System.currentTimeMillis());
            dbMessagesRef.push().setValue(msg, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Toast.makeText(TextMessageActivity.this,
                                "Could not send your response. Error code: " +
                                        databaseError.getCode(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, databaseError.getCode() + ": " + databaseError.getMessage() +
                                " Key: " + databaseReference.getKey());
                    } else {
                        Toast.makeText(TextMessageActivity.this, "Thank you for your response",
                                Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Message sent");
                    }
                }
            });
            finish();
        } else {
            Toast.makeText(this, "Enable location before sending your response", Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "location not available");
        }
    }

    private void addAuthStateListener() {
        if (userAuthListener == null)
            userAuthListener = getUserAuthState();
        userAuth.addAuthStateListener(userAuthListener);
        Log.i(TAG, "auth listener added");
    }

    private void removeAuthStateListener() {
        if (userAuthListener != null) {
            userAuth.removeAuthStateListener(userAuthListener);
            userAuthListener = null;
            Log.i(TAG, "auth listener removed");
        }
    }

    private FirebaseAuth.AuthStateListener getUserAuthState() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    session = new UserSession(getApplicationContext());
                    removeAuthStateListener();
                }
            }
        };
    }

    private final class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().get("location") != null)
                location = (Location) intent.getExtras().get("location");
        }
    }
}
