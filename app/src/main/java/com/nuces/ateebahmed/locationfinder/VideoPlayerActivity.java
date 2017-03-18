package com.nuces.ateebahmed.locationfinder;

import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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

import models.Message;

public class VideoPlayerActivity extends AppCompatActivity implements
        TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener,
        MediaController.MediaPlayerControl {

    private static final String TAG = "VideoPlayerActivity";
    private TextureView videoView;
    private FloatingActionButton btnVideoSend;
    private MediaPlayer videoPlayer;
    private Surface s;
    private MediaController videoController;
    private Handler handler;
    private StorageReference mediaStorageRef, videoStorageRef;
    private DatabaseReference dbMessagesRef;
    private Uri videoFilePath;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userAuthListener;
    private UserSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Toolbar simpleBar = (Toolbar) findViewById(R.id.simpleBar);
        setSupportActionBar(simpleBar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        videoFilePath = Uri.parse(getIntent().getStringExtra("path"));
        userAuth = FirebaseAuth.getInstance();
        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");
        mediaStorageRef = FirebaseStorage.getInstance().getReference();

        videoStorageRef = mediaStorageRef.child("videos");

        handler = new Handler();

        videoView = (TextureView) findViewById(R.id.videoPlayer);
        videoView.setSurfaceTextureListener(this);
        btnVideoSend = (FloatingActionButton) findViewById(R.id.btnVideoSend);
        btnVideoSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadVideoToStorage();
            }
        });

        videoController = new MediaController(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        s = new Surface(surfaceTexture);
        setMediaPlayer(videoFilePath.getPath());
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        s.release();
        s = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onPrepared(final MediaPlayer videoPlayer) {
        videoController.setMediaPlayer(this);
        videoController.setAnchorView(videoView);
        handler.post(new Runnable() {
            @Override
            public void run() {
                videoController.setEnabled(true);
                videoController.show();
            }
        });
        videoPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        videoController.hide();
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.release();
            videoPlayer = null;
        }
    }

    @Override
    public void start() {
        videoPlayer.start();
    }

    @Override
    public void pause() {
        videoPlayer.pause();
    }

    @Override
    public int getDuration() {
        return videoPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return videoPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        videoPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return videoPlayer.isPlaying();
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
    public boolean onTouchEvent(MotionEvent event) {
        videoController.show();
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        addAuthStateListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeAuthStateListener();
    }

    private void setMediaPlayer(String path) {
        videoPlayer = new MediaPlayer();
        videoPlayer.setOnPreparedListener(this);
        try {
            videoPlayer.setDataSource(path);
            videoPlayer.setSurface(s);
            videoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            videoPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadVideoToStorage() {
        videoStorageRef.child(videoFilePath.getLastPathSegment())
                .putFile(Uri.fromFile(new File(videoFilePath.getPath()))).addOnSuccessListener(this,
                new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getDownloadUrl() != null) {
                            if (session.isLoggedIn()) {
                                /*Message msg = new Message(session.getSPUsername(),
                                        loc.getLongitude(), loc.getLatitude(),
                                        System.currentTimeMillis());
                                msg.setVideo(taskSnapshot.getDownloadUrl().toString());

                                dbMessagesRef.push().setValue(msg);*/

                                Toast.makeText(VideoPlayerActivity.this, "Thank you! Your response "
                                        + "has been recorded", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(VideoPlayerActivity.this, "There was an error uploading your response",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, e.getMessage());
            }
        });
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
}
