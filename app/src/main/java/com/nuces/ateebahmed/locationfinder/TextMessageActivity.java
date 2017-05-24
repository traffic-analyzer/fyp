package com.nuces.ateebahmed.locationfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import models.Message;

public class TextMessageActivity extends AppCompatActivity {

    private static final String TAG = "TextMessageActivity";
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver locationReceiver;
    private DatabaseReference dbMessagesRef;
    private UserSession session;
    private Location location;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_message);

        String[] trafficSituation = getResources().getStringArray(R.array.traffic_situation);
        TypedArray icons = getResources().obtainTypedArray(R.array.icons);

        ListView lvSituation = (ListView) findViewById(R.id.lvSituation);

        TrafficSituationListAdapter listAdapter =
                new TrafficSituationListAdapter(getApplicationContext(), trafficSituation, icons);

        lvSituation.setAdapter(listAdapter);
        lvSituation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(), ((AppCompatTextView)((LinearLayoutCompat)
                        view).getChildAt(1)).getText().toString(), Toast.LENGTH_SHORT).show();
                /*sendMessage((((AppCompatTextView) ((LinearLayoutCompat) view)
                        .getChildAt(1)).getText().toString()));*/
            }
        });

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        locationReceiver = locationReceiver();

        userAuth = FirebaseAuth.getInstance();

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");

    }

    @Override
    protected void onResume() {
        super.onResume();
        addAuthStateListener();
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
        if (LocationComponentsSingleton.getInstance(getApplicationContext()).isLocationAvailable())
            setLocation(LocationComponentsSingleton.getInstance(getApplicationContext())
                    .getLocation());
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(locationReceiver);
        removeAuthStateListener();
    }

    private void sendMessage(String b) {
        if (location != null && session != null) {
            Message msg = new Message(b, session.getSPUsername(),
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
                if (firebaseAuth.getCurrentUser() != null && session == null)
                    session = new UserSession(getApplicationContext());
            }
        };
    }

    private void setLocation(Location location) {
        this.location = location;
    }

    private BroadcastReceiver locationReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getExtras().get("location") != null)
                    setLocation((Location)intent.getExtras().get("location"));
            }
        };
    }
}
