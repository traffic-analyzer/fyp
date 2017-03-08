package com.nuces.ateebahmed.locationfinder;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.Message;

public class ImageViewActivity extends AppCompatActivity {

    private static final String TAG = "ImageViewActivity";
    private AppCompatImageView imageView;
    private FloatingActionButton btnImageSend;
    private static final String IMG_DIR_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/DCIM/LocationFinder";
    private StorageReference mediaStorageRef, imageStorageRef, videoStorageRef;
    private LocationComponentsSingleton instance;
    private UserSession session;
    private Location loc;
    private DatabaseReference dbMessagesRef;
    private BroadcastReceiver locationBroadcastReceiver;
    private byte[] image;
    private int orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Toolbar simpleBar = (Toolbar) findViewById(R.id.simpleBar);
        setSupportActionBar(simpleBar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = (AppCompatImageView) findViewById(R.id.imageView);
        btnImageSend = (FloatingActionButton) findViewById(R.id.btnImageSend);

        image = getIntent().getByteArrayExtra("image");
        orientation = getIntent().getIntExtra("orientation", 0);
        showImage(image);
        instance = LocationComponentsSingleton.getInstance(getApplicationContext());

        locationBroadcastReceiver = new ImageViewActivity.LocationBroadcastReceiver();

        dbMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages");

        session = new UserSession(getApplicationContext());

        mediaStorageRef = FirebaseStorage.getInstance().getReference();

        imageStorageRef = mediaStorageRef.child("images");

        videoStorageRef = mediaStorageRef.child("videos");

        btnImageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageOnLocalStorage(image, orientation);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(locationBroadcastReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);
    }

    private void showImage(byte[] data) {
        Matrix mat = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                mat.postRotate(90f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                mat.postRotate(180f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                mat.postRotate(270f);
                break;
        }
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(),
                imageBitmap.getHeight(), mat, true);
        imageView.setImageBitmap(imageBitmap);
    }

    private void saveImageOnLocalStorage(byte[] data, int orientation) {
        String imageName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date()) + ".jpg";

        File imageFile = new File(IMG_DIR_PATH + "/" + imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            fos.write(data);
            fos.close();
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
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

        ImageViewActivity.this.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Log.i(TAG, "captured");

        uploadPictureToStorage(imageFile);
    }

    private void uploadPictureToStorage(File imageFile) {
        imageStorageRef.child(imageFile.getName()).putFile(Uri.fromFile(imageFile))
                .addOnSuccessListener(ImageViewActivity.this,
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                            {
                                if (taskSnapshot.getDownloadUrl() != null) {
                                    if (session.isLoggedIn()) {
                                        Message msg = new Message(session.getSPUsername(),
                                                loc.getLongitude(), loc.getLatitude(),
                                                System.currentTimeMillis());
                                        msg.setImage(taskSnapshot.getDownloadUrl()
                                                .toString());

                                        dbMessagesRef.push().setValue(msg);

                                        Toast.makeText(ImageViewActivity.this,
                                                "Thank you! Your response has been recorded",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }).addOnFailureListener(ImageViewActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ImageViewActivity.this,
                        "There was a problem in uploading your response!",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, e.getMessage());
            }
        });
    }

    private void createDirs() {
        File mkDir = new File(IMG_DIR_PATH);
        if (!mkDir.exists()) {
            if (!mkDir.mkdirs()) {
                Log.w(TAG, "could not make directories");
            }
        }
    }

    private final class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().get("location") != null) {
                loc = (Location) intent.getExtras().get("location");
            }
        }
    }
}
