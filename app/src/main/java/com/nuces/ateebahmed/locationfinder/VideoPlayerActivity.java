package com.nuces.ateebahmed.locationfinder;

import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoPlayer;
    private FloatingActionButton btnVideoSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Toolbar simpleBar = (Toolbar) findViewById(R.id.simpleBar);
        setSupportActionBar(simpleBar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        videoPlayer = (VideoView) findViewById(R.id.videoPlayer);
        btnVideoSend = (FloatingActionButton) findViewById(R.id.btnVideoSend);

        String path = getIntent().getStringExtra("path");
        videoPlayer.setVideoURI(Uri.parse(path));
        MediaController mc = new MediaController(this);
        videoPlayer.setMediaController(mc);
    }
}
