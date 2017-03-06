package com.nuces.ateebahmed.locationfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.Message;

public class VoiceRecorderActivity extends AppCompatActivity {

    private static final String TAG = "TextMessageActivity",
            AUD_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/LocationFinder";
    private LocationComponentsSingleton instance;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver locationReceiver;
    private DatabaseReference dbMessagesRef;
    private UserSession session;
    private Location location;
    private SwitchCompat swtchRecord;
    private final int REQUEST_CODE_AUDIO = 3;
    private MediaRecorder audioRecorder;
    private File audioFile;
    private StorageReference audioStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recorder);

        setInstance();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        locationReceiver = new VoiceRecorderActivity.LocationBroadcastReceiver();

        session = new UserSession(getApplicationContext());

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");
        audioStorageRef = FirebaseStorage.getInstance().getReference().child("audio");

        swtchRecord = (SwitchCompat) findViewById(R.id.swtchRecord);

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

    private void setInstance() {
        if (instance == null) {
            instance = LocationComponentsSingleton.getInstance(this);
        }
    }

    private boolean isRecordingAllowed() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }

    private void getRecordingPermission() {
        if (!isRecordingAllowed())
            ActivityCompat.requestPermissions(this, new String[]
                            {android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO);
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
        createDirs();
        audioFile = new File(AUD_DIR_PATH + "/AUD_" + new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date()) + ".3gp");
        audioRecorder.setOutputFile(audioFile.getAbsolutePath());
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

        uploadAudioToStorage();
    }

    private void createDirs() {
        File mkDir = new File(AUD_DIR_PATH);
        if (!mkDir.exists()) {
            if (!mkDir.mkdirs()) {
                Log.w(TAG, "could not make directories");
            }
        }
    }

    private void uploadAudioToStorage() {
        audioStorageRef.child(audioFile.getName()).putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getDownloadUrl() != null)
                            if (session.isLoggedIn()) {
                                Message msg = new Message(session.getSPUsername(),
                                        location.getLongitude(), location.getLatitude(),
                                        System.currentTimeMillis());
                                msg.setAudio(taskSnapshot.getDownloadUrl().toString());
                                dbMessagesRef.push().setValue(msg);

                                Toast.makeText(VoiceRecorderActivity.this,
                                        "Thank you! Your response has been recorded",
                                        Toast.LENGTH_LONG).show();
                            }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(VoiceRecorderActivity.this,
                        "There was a problem in uploading your response!", Toast.LENGTH_LONG).show();
                Log.e(TAG, e.getMessage());
            }
        });
    }

    private final class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().get("location") != null)
                location = (Location) intent.getExtras().get("location");
        }
    }
}
