package com.nuces.ateebahmed.locationfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import models.Message;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private Button btnBlocked, btnSlow, btnNormal, btnSpeedy, btnNone;
    private LocationComponentsSingleton instance;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver locationReceiver;
    private DatabaseReference dbMessagesRef;
    private UserSession session;
    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setInstance();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        locationReceiver = new LocationBroadcastReceiver();

        session = new UserSession(getApplicationContext());

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");


        btnBlocked = (Button) findViewById(R.id.btnBlocked);
        btnSlow = (Button) findViewById(R.id.btnSlow);
        btnNormal = (Button) findViewById(R.id.btnNormal);
        btnSpeedy = (Button) findViewById(R.id.btnSpeedy);
        btnNone = (Button) findViewById(R.id.btnNone);

        btnBlocked.setOnClickListener(onButtonPressed());
        btnSlow.setOnClickListener(onButtonPressed());
        btnNormal.setOnClickListener(onButtonPressed());
        btnSpeedy.setOnClickListener(onButtonPressed());
        btnNone.setOnClickListener(onButtonPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(locationReceiver);
    }

    private View.OnClickListener onButtonPressed() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btnBlocked:
                        sendMessage((Button) view);
                        break;
                    case R.id.btnSlow:
                        sendMessage((Button) view);
                        break;
                    case R.id.btnNormal:
                        sendMessage((Button) view);
                        break;
                    case R.id.btnSpeedy:
                        sendMessage((Button) view);
                        break;
                    case R.id.btnNone:
                        sendMessage((Button) view);
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

    private void sendMessage(Button b) {
        if (location != null) {
            Message msg = new Message(b.getText().toString(), session.getSPUsername(),
                    location.getLongitude(), location.getLatitude(), System.currentTimeMillis());
            dbMessagesRef.push().setValue(msg, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Toast.makeText(ChatActivity.this,
                                "Could not send your response. Error code: " +
                                        databaseError.getCode(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, databaseError.getCode() + ": " + databaseError.getMessage() +
                                " Key: " + databaseReference.getKey());
                    } else {
                        Toast.makeText(ChatActivity.this, "Thank you for your response",
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

    private final class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().get("location") != null)
                location = (Location) intent.getExtras().get("location");
        }
    }
}
