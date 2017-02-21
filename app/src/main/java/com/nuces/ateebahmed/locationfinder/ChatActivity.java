package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private SwitchCompat swtchRecord;
    private final int REQUEST_CODE_AUDIO = 3;
    private MediaRecorder audioRecorder;

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

        swtchRecord = (SwitchCompat) findViewById(R.id.swtchRecord);

        btnBlocked.setOnClickListener(onButtonPressed());
        btnSlow.setOnClickListener(onButtonPressed());
        btnNormal.setOnClickListener(onButtonPressed());
        btnSpeedy.setOnClickListener(onButtonPressed());
        btnNone.setOnClickListener(onButtonPressed());
        swtchRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    getRecordingPermission();
                else stopRecording();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (audioRecorder == null)
            initRecorder();
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioRecorder != null) {
            audioRecorder.release();
            audioRecorder = null;
        }
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
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {
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

    private boolean isRecordingAllowed() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }

    private void getRecordingPermission() {
        if (!isRecordingAllowed())
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    REQUEST_CODE_AUDIO);
        else startRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i(TAG, "permission granted");
                    startRecording();
                } else {
                    Log.e(TAG, "permission denied");
                    Toast.makeText(this, "Allow audio permission to record and send audio messages",
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void initRecorder() {
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    private void startRecording() {
        audioRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/AUD_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".3gp");
        try {
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            audioRecorder.release();
        }
    }

    private void stopRecording() {
        audioRecorder.stop();
        audioRecorder.release();
    }

    private final class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().get("location") != null)
                location = (Location) intent.getExtras().get("location");
        }
    }
}
