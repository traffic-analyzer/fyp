package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    private final String TAG = "CameraActivity";
    private CameraPreview preview;
    private Camera camera;
    private AppCompatImageButton flashCameraButton, captureImage, record;
    private boolean flashmode, camMode;
    private int rotation, orientation;
    private FrameLayout cameraView;
    private SensorManager sensorManager;
    private MediaRecorder videoRecorder;
    private final int CAMERA_REQUEST_CODE = 1;
    private String videoFileName;
    private SwitchCompat swtCameraMode;
    private static final String IMG_DIR_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/DCIM/LocationFinder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_camera);

        camMode = false;

        flashCameraButton = (AppCompatImageButton) findViewById(R.id.flash);
        captureImage = (AppCompatImageButton) findViewById(R.id.captureImage);
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage();
            }
        });
        
        record = (AppCompatImageButton) findViewById(R.id.btnRecord);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        swtCameraMode = (SwitchCompat) findViewById(R.id.swtCameraMode);
        swtCameraMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                changeCameraMode(b);
            }
        });

        flashmode = false;

        if (!getBaseContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        } else flashCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flashOnButton();
            }
        });
        orientation = ExifInterface.ORIENTATION_ROTATE_90;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!areCameraPermissionsAllowed())
            requestForCamera();
        else {
            // camera surface view created
            cameraView = (FrameLayout) findViewById(R.id.surfaceView);
            createCamera();
            sensorManager.registerListener(this, sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        camera = null;
        if(videoRecorder != null)
            videoRecorder.release();
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        Camera.Parameters params = c.getParameters();

        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;

            default:
                break;
        }

        rotation = (info.orientation - degree + 360) % 360;
        c.setDisplayOrientation(rotation);

        showFlashButton(params);

        params.setRotation(rotation);

        c.setParameters(params);
    }

    private void showFlashButton(Camera.Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
                && params.getSupportedFlashModes() != null
                && params.getSupportedFlashModes().size() > 1;

        flashCameraButton.setVisibility(showFlash ? View.VISIBLE
                : View.GONE);

    }

    private void takeImage() {
        record.setEnabled(false);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                if (b) {
                    camera = CameraActivity.this.camera;
                    camera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {
                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            am.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
                        }
                    }, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {

                            Intent imageData = new Intent(CameraActivity.this,
                                    ImageViewActivity.class);
                            imageData.putExtra("image", data);
                            imageData.putExtra("orientation", orientation);
                            startActivity(imageData);
                            finish();
//                            showImage(data);

//                            saveImageOnLocalStorage(data);
                        }
                    });
                } else Log.e(TAG, "could not focus");
            }
        });

        record.setEnabled(true);
    }

    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(CameraActivity.this,
                "Camera info", "error to open camera");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });

        dialog.show();
    }

    private AlertDialog.Builder createAlert(Context context, String title, String message) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(context,
                        android.R.style.Theme_Dialog));
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle("Information");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;

    }

    private void flashOnButton() {
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_ON
                        : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
                changeFlashIcon(flashmode);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                    if (sensorEvent.values[1] > 0 && orientation !=
                            ExifInterface.ORIENTATION_ROTATE_90) {
                        // UP
                        orientation = ExifInterface.ORIENTATION_ROTATE_90;
                        Log.i(TAG, "facing up");
                    } else if (sensorEvent.values[1] < 0 && orientation !=
                            ExifInterface.ORIENTATION_ROTATE_270) {
                        // UP SIDE DOWN
                        orientation = ExifInterface.ORIENTATION_ROTATE_270;
                        Log.i(TAG, "facing down");
                    }
                } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                    if (sensorEvent.values[0] > 0 && orientation !=
                            ExifInterface.ORIENTATION_NORMAL) {
                        // LEFT
                        orientation = ExifInterface.ORIENTATION_NORMAL;
                        Log.i(TAG, "facing left");
                    } else if (sensorEvent.values[0] < 0 && orientation !=
                            ExifInterface.ORIENTATION_ROTATE_180) {
                        // RIGHT
                        orientation = ExifInterface.ORIENTATION_ROTATE_180;
                        Log.i(TAG, "facing right");
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void createCamera() {
        camera = getCameraInstance();

        if (camera == null) {
            alertCameraDialog();
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureSize(1920, 1080);
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setJpegQuality(80);

        setUpCamera(camera);

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }

        camera.setParameters(parameters);

        preview = new CameraPreview(this, camera);
        
        initRecorder();

        cameraView.addView(preview);

//        createDirs();
    }

    private boolean areCameraPermissionsAllowed() {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestForCamera() {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.length > 0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                createCamera();
                sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Allow permission to take and save pictures",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "permission denied");
                finish();
            }
        }
    }

    private void initRecorder() {
        videoRecorder = new MediaRecorder();
        videoRecorder.setCamera(camera);
        videoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        videoRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        videoRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        videoRecorder.setMaxDuration(10000);
    }

    private void prepareRecorder() {
        videoFileName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
        videoRecorder.setOrientationHint(getWindowManager().getDefaultDisplay().getRotation());
        videoRecorder.setPreviewDisplay(preview.getSurfaceHolder().getSurface());
        videoRecorder.setOutputFile(IMG_DIR_PATH + "/" + videoFileName);
        try {
            videoRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        if (!camMode) {
            camMode = true;
            captureImage.setEnabled(false);
            camera.unlock();
            prepareRecorder();
            videoRecorder.start();
            record.setImageResource(R.drawable.ic_record_off);
        } else {
            camMode = false;
            videoRecorder.stop();
            camera.lock();
            record.setImageResource(R.drawable.ic_record_on);
            captureImage.setEnabled(true);

            Intent video = new Intent(this, VideoPlayerActivity.class);
            video.putExtra("path", IMG_DIR_PATH + "/" + videoFileName);
            startActivity(video);
            finish();

//            uploadVideoToStorage();

//            initRecorder();
        }
    }

    private void changeCameraMode(boolean state) {
        if (state) {
            record.setVisibility(View.VISIBLE);
            captureImage.setVisibility(View.GONE);
        } else {
            captureImage.setVisibility(View.VISIBLE);
            record.setVisibility(View.GONE);
        }
    }

    private void changeFlashIcon(boolean mode) {
        if (mode) flashCameraButton.setImageResource(R.drawable.ic_flash_off);
        else flashCameraButton.setImageResource(R.drawable.ic_flash_on);
    }
}