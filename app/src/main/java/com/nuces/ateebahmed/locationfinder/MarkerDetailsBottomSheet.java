package com.nuces.ateebahmed.locationfinder;

import android.app.Dialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Created by progamer on 10/04/17.
 */

public class MarkerDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "MrkerDetailsBottomSheet";
    private AppCompatTextView txtMessage;
    private AppCompatImageView imgResult;
    private VideoView videoSurface;
    private MediaController videoController;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
    private FrameLayout videoFrame;
    private MediaPlayer audioPlayer;

    public MarkerDetailsBottomSheet() {}

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    dismiss();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        };
        return inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtMessage = (AppCompatTextView) view.findViewById(R.id.txtMessage);
        imgResult = (AppCompatImageView) view.findViewById(R.id.imgResult);
        videoSurface = (VideoView) view.findViewById(R.id.videoSurface);
        videoFrame = (FrameLayout) view.findViewById(R.id.videoFrame);
        audioPlayer = new MediaPlayer();
        String uri = getArguments().getString("msg");
        setMediaResource(uri);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        View view = View.inflate(getContext(), R.layout.fragment_bottom_sheet_dialog, null);
        dialog.setContentView(view);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View)
                view.getParent()).getLayoutParams();
        BottomSheetBehavior behavior = (BottomSheetBehavior) params.getBehavior();
        if (behavior != null)
            behavior.setBottomSheetCallback(bottomSheetCallback);
    }

    public static MarkerDetailsBottomSheet newInstance(String message) {
        MarkerDetailsBottomSheet markerDetails = new MarkerDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("msg", message);
        markerDetails.setArguments(args);
        return markerDetails;
    }

    private void setMediaResource(String uri) {
        if (uri.toLowerCase().contains("image")) {
            Picasso.with(getContext()).load(uri).into(imgResult);
            imgResult.setVisibility(View.VISIBLE);
        } else if(uri.toLowerCase().contains("video")) {
            setVideoPlayer(Uri.parse(uri));
        } else if (uri.toLowerCase().contains("audio")) {
            setAudioPlayer(Uri.parse(uri));
        } else {
            txtMessage.setText(uri);
            txtMessage.setVisibility(View.VISIBLE);
        }
    }

    private void setVideoPlayer(Uri uri) {
        videoSurface.setVideoURI(uri);
        videoSurface.setVisibility(View.VISIBLE);
        setVideoController(videoSurface);
        videoSurface.setMediaController(videoController);
        videoSurface.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoSurface.start();
            }
        });
        videoSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoController.show();
            }
        });
    }

    private void setVideoController(View view) {
        videoController = new MediaController(getActivity());
        FrameLayout.LayoutParams lp = new FrameLayout
                .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        videoController.setLayoutParams(lp);
        ((ViewGroup) videoController.getParent()).removeView(videoController);
        videoFrame.addView(videoController);
        videoController.setAnchorView(view);
    }

    private void setAudioPlayer(Uri uri) {
        try {
            audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            audioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    mediaPlayer.reset();
                    return false;
                }
            });
            audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    audioPlayer.release();
                    if (audioPlayer != null)
                        audioPlayer = null;
                    dismiss();
                }
            });
            audioPlayer.setDataSource(getActivity(), uri);
            setVideoController(videoFrame);
            videoController.setMediaPlayer(new MediaController.MediaPlayerControl() {
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
                    if (audioPlayer.getCurrentPosition() > 0 &&
                            audioPlayer.getCurrentPosition() < audioPlayer.getDuration())
                        return audioPlayer.getCurrentPosition();
                    else return 0;
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
                    return (getCurrentPosition() / audioPlayer.getDuration()) * 100;
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
                    return audioPlayer.getAudioSessionId();
                }
            });
            audioPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}