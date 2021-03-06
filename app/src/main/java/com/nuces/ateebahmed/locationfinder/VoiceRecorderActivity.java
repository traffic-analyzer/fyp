package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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

public class VoiceRecorderActivity extends AppCompatActivity implements
        MediaController.MediaPlayerControl, MediaPlayer.OnPreparedListener {

    private static final String TAG = "VoiceRecorderActivity",
            AUD_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/DCIM/LocationFinder";
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver locationReceiver;
    private DatabaseReference dbMessagesRef;
    private UserSession session;
    private Location location;
    private final int REQUEST_CODE_AUDIO = 3;
    private MediaRecorder audioRecorder;
    private File audioFile;
    private StorageReference audioStorageRef;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;
    private Handler handler;
    private MediaController audioController;
    private MediaPlayer audioPlayer;
    private AppCompatImageButton btnAudSend, btnVoiceRecord;
    private AppCompatTextView timer;
    private boolean isRecording;
    private CountDownTimer clockTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recorder);

        handler = new Handler();

        isRecording = false;

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        locationReceiver = locationReceiver();

        if (LocationComponentsSingleton.getInstance(getApplicationContext()).isLocationAvailable())
            location = LocationComponentsSingleton.getInstance(getApplicationContext())
                    .getLocation();

        userAuth = FirebaseAuth.getInstance();

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");
        audioStorageRef = FirebaseStorage.getInstance().getReference().child("audio");

        btnAudSend = (AppCompatImageButton) findViewById(R.id.btnAudSend);
        btnAudSend.setEnabled(false);
        btnAudSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                uploadAudioToStorage();
            }
        });

        btnVoiceRecord = (AppCompatImageButton) findViewById(R.id.btnVoiceRecord);
        btnVoiceRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording)
                    startRecording();
                else {
                    clockTimer.cancel();
                    stopRecording();
                }
            }
        });

        timer = (AppCompatTextView) findViewById(R.id.timer);

        audioPlayer = new MediaPlayer();
        audioPlayer.setOnPreparedListener(this);
        audioController = new MediaController(this) {
            @Override
            public void hide() {}

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    super.hide();
                    ((AppCompatActivity) getContext()).finish();
                }
                return super.dispatchKeyEvent(event);
            }
        };
        audioController.setMediaPlayer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        addAuthStateListener();
        getRecordingPermission();
        localBroadcastManager.registerReceiver(locationReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
    }

    private boolean isRecordingAllowed() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED);
    }

    private void initRecorder() {
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    private void getRecordingPermission() {
        if (!isRecordingAllowed())
            ActivityCompat.requestPermissions(this, new String[]
                    {android.Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_AUDIO);
        else if (audioRecorder == null)
            initRecorder();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_AUDIO:
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i(TAG, "permission granted");
                    if (audioRecorder == null)
                        initRecorder();
                } else {
                    Log.e(TAG, "permission denied");
                    Toast.makeText(this, "Allow audio permission to record and send audio messages",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
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

    private void startRecording() {
        createDirs();
        audioFile = new File(AUD_DIR_PATH + "/AUD_" + new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date()) + ".3gp");
        audioRecorder.setOutputFile(audioFile.getAbsolutePath());
        try {
            audioRecorder.prepare();
            audioRecorder.start();
            isRecording = true;
            btnVoiceRecord.setImageResource(R.drawable.mic_recording);
            clockTimer = new CountDownTimer(30000, 1000) {
                @Override
                public void onTick(long l) {
                    long sec = l / 1000;
                    if (sec < 10) timer.setText("00:0" + String.valueOf(l/1000));
                    else timer.setText("00:" + String.valueOf(l/1000));
                }

                @Override
                public void onFinish() {
                    timer.setText("00:00");
                    stopRecording();
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
            audioRecorder.release();
        }
    }

    private void stopRecording() {
        isRecording = false;
        btnVoiceRecord.setImageResource(R.drawable.micc);
        audioRecorder.stop();
        audioRecorder.release();

        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            audioPlayer.setDataSource(audioFile.getPath());
            audioPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        btnAudSend.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioRecorder != null) {
            audioRecorder.release();
            audioRecorder = null;
        }
        localBroadcastManager.unregisterReceiver(locationReceiver);
        removeAuthStateListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    @Override
    public void start() {
        audioPlayer.start();
    }

    @Override
    public void pause() {
        audioPlayer.pause();
    }

    @Override
    public int getDuration() {
        return audioPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return audioPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        audioPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return audioPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        audioController.setAnchorView(findViewById(R.id.audioController));
        handler.post(new Runnable() {
            @Override
            public void run() {
                audioController.setEnabled(true);
                audioController.show(0);
            }
        });
    }

    private void addAuthStateListener() {
        if (userAuthListener == null)
            userAuthListener = getUserAuthState();
        userAuth.addAuthStateListener(userAuthListener);
    }

    private void removeAuthStateListener() {
        if (userAuthListener != null) {
            userAuth.removeAuthStateListener(userAuthListener);
            userAuthListener = null;
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
                    setLocation((Location) intent.getExtras().get("location"));
            }
        };
    }
}
