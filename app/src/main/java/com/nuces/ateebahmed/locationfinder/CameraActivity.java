package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    private final String TAG = "CameraActivity";
    private CameraPreview preview;
    private Camera camera;
    private Button flashCameraButton;
    private Button captureImage;
    private boolean flashmode;
    private int rotation, orientation;
    private FrameLayout cameraView;
    private SensorManager sensorManager;
    private LocationComponentsSingleton instance;
    private final int CAMERA_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        instance = LocationComponentsSingleton.getInstance(getApplicationContext());

        // camera surface view created
        cameraView = (FrameLayout) findViewById(R.id.surfaceView);
        flashCameraButton = (Button) findViewById(R.id.flash);
        captureImage = (Button) findViewById(R.id.captureImage);
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImage();
            }
        });
        flashmode = false;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!getBaseContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }
        orientation = ExifInterface.ORIENTATION_ROTATE_90;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!areCameraPermissionsAllowed())
            requestForCamera();
        else {
            createCamera();
            sensorManager.registerListener(this, sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        Camera.Parameters params = c.getParameters();
//        params.setPictureSize (3264, 2448);
// params.setPictureSize(height, width); 3264 x 2448 -> 8MP

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
        //Parameters params = c.getParameters();

//        params.setPictureSize (1920, 2560);
//
//        c.setParameters(params);

        showFlashButton(params);

        params.setRotation(rotation);

        c.setParameters(params);
    }

    private void showFlashButton(Camera.Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
                && params.getSupportedFlashModes() != null
                && params.getSupportedFocusModes().size() > 1;

        flashCameraButton.setVisibility(showFlash ? View.VISIBLE
                : View.INVISIBLE);

    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            camera = null;
        }
    }

    /*@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flash:
                flashOnButton();
                break;
            case R.id.captureImage:
                takeImage();
                break;

            default:
                break;
        }
    }*/

    private void takeImage() {
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

                        private File imageFile;

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            String imageName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss")
                                    .format(new Date()) + ".jpg";
                            File mkDir = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath(), "/DCIM/LocationFinder");
                            if (!mkDir.exists()) {
                                if (!mkDir.mkdirs()) {
                                    Log.i(TAG, "could not make directories");
                                    return;
                                }
                            }

                            imageFile = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath(), "/DCIM/LocationFinder/" + imageName);

                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(imageFile);
                                fos.write(data);
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                ExifInterface exif = new ExifInterface(Environment
                                        .getExternalStorageDirectory() + "/DCIM/LocationFinder/" +
                                        imageName);
                                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                                        String.valueOf(orientation));
                                exif.saveAttributes();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            ContentValues values = new ContentValues();

                            values.put(MediaStore.Images.Media.DATE_TAKEN,
                                    System.currentTimeMillis());
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            values.put(MediaStore.MediaColumns.DATA,
                                    imageFile.getAbsolutePath());

                            CameraActivity.this.getContentResolver().insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            Log.i(TAG, "captured");

                        }
                    });
                } else Log.e(TAG, "could not focus");
            }
        });
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
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH
                        : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
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

        cameraView.addView(preview);
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
            }
        }
    }
}