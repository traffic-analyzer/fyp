package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by progamer on 15/2/17.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private final String TAG = "CameraPreview";
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    // only because AS gives warning which I don't like to see
    private CameraPreview(Context c) {
        super(c);
    }

    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setFixedSize(100, 100);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            Log.i(TAG, "created");
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(this.surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (this.surfaceHolder.getSurface() == null)
            return;
        camera.stopPreview();
        try {
            camera.setPreviewDisplay(this.surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.i(TAG, "destroyed");
        camera.stopPreview();
        camera.release();
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }
}
