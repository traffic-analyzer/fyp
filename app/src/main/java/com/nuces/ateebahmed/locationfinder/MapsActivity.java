package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import models.User;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        ResultCallback<LocationSettingsResult> {

    protected static final String ACTION = "com.nuces.ateebahmed.locationfinder.MapsActivity";
    private static final String TAG = "MapsActivity";

    private BroadcastReceiver locationBroadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private LocationComponentsSingleton instance;
    protected GoogleApiClient gClient;
    protected LocationSettingsRequest locSettingReq;
    protected Location loc;
    protected boolean locUpd, geofenceAdded, isBtnTapped, isConnected;
    private SharedPreferences sharedPreferences;
    private ResultCallback<Status> statusResult;

    private GoogleMap mMap;
    protected Marker marker;
    protected Circle circle;
    private ArrayList<Marker> userMarkers;
    protected ArrayList<Geofence> geofenceList;

    private DatabaseReference dbUsersRef, conRef;
    private ValueEventListener connectionListener;
    private ChildEventListener usersLocationListener;

    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userVerified;

    private Toolbar searchBar;
    private FloatingActionButton btnGotoMarker, btnAddContent, btnCamera, btnChat, btnVoiceRecord;
    private Animation animBtnOpen, animBtnClose, animRotateForward, animRotateBackward;

    // Constants
    private String packageName = "com.nuces.ateebahmed.locationfinder",
            sharedPreferencesName = packageName + ".SHARED_PREFERENCES_NAME",
            geofencesAddedKey = packageName + ".GEOFENCES_ADDED_KEY";
    private long geofenceExpiration = 60 * 60 * 1000, geofenceRadius = 100;
    protected static final int CHECK_SETTINGS = 0x1, ENABLE_LOCATION = 0x2;
    private static final String GEOFENCE_REQUEST_KEY = "own";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        isConnected = false;

        searchBar = (Toolbar) findViewById(R.id.searchBar);
        setSupportActionBar(searchBar);
        btnGotoMarker = (FloatingActionButton) findViewById(R.id.btnGotoMarker);
        btnGotoMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMarker();
            }
        });
        btnAddContent = (FloatingActionButton) findViewById(R.id.btnAddContent);
        btnAddContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
                Log.i("MAPS", "works");
            }
        });
        btnCamera = (FloatingActionButton) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("MAPS", "Camera");
                animateBtnAddContent();
            }
        });
        btnChat = (FloatingActionButton) findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
                Log.i("MAPS", "Chat");
                Intent chat = new Intent(getApplicationContext(), TextMessageActivity.class);
                startActivity(chat);
            }
        });
        btnVoiceRecord = (FloatingActionButton) findViewById(R.id.btnVoiceRecord);
        btnVoiceRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
                Intent voice = new Intent(getApplicationContext(), VoiceRecorderActivity.class);
                startActivity(voice);
            }
        });

        animBtnOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_open);
        animBtnClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_close);
        animRotateForward = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_forward);
        animRotateBackward = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_backward);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofenceList = new ArrayList<>();
        statusResult = getStatusResult();
        userMarkers = new ArrayList<>();
        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        geofenceAdded = sharedPreferences.getBoolean(geofencesAddedKey, false);
        locUpd = false;
        marker = null;
        circle = null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Move the camera to Karachi, PK
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(24.8615, 67.0099), 10));

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(0, 0);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    protected void onStart() {
        super.onStart();

        setInstance();

        addConnectionListener();

        getInstances();

        userVerified = getUserVerified();

        if (isConnected)
            userAuth.addAuthStateListener(userVerified);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setLocationBroacastReceiver();

        startBackgroundService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        userAuth.removeAuthStateListener(userVerified);
        removeConnectionListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendLocationUpdateSignal(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        localBroadcastManager.unregisterReceiver(locationBroadcastReceiver);
        detachUsersListener();
        if (!geofenceList.isEmpty()) {
            geofenceList.remove(0);
            removeGeofence();
        }
        removeUserMarkers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gClient.isConnected() && locUpd) {
            attachUsersListener();
        }
        localBroadcastManager.registerReceiver(locationBroadcastReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
        instance.setLocationPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        checkLocSettings();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                sendLocationUpdateSignal(LocationRequest.PRIORITY_HIGH_ACCURACY);
                Toast.makeText(this, "Location enabled", Toast.LENGTH_SHORT).show();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                try {
                    status.startResolutionForResult(MapsActivity.this, ENABLE_LOCATION);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e(TAG, "Location settings cannot be done");
                Toast.makeText(this, "Enable Location in Setttings", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Activity.RESULT_OK:
                sendLocationUpdateSignal(LocationRequest.PRIORITY_HIGH_ACCURACY);
                Log.i(TAG, "Okay");
                break;
            case Activity.RESULT_CANCELED:
                Log.e(TAG, "cancelled");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_search, menu);

        MenuItemCompat.OnActionExpandListener searchExpandListener =
                new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.i("MAPS", item.getItemId() + " expanded");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.i("MAPS", item.getItemId() + " collapsed");
                return true;
            }
        };

        MenuItem searchItem = menu.findItem(R.id.search);

        MenuItemCompat.setOnActionExpandListener(searchItem, searchExpandListener);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                Log.i("MAPS", "okay");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case CHECK_SETTINGS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i(TAG, "rechecking permission");
                    checkLocSettings();
                } else {
                    Toast.makeText(this, "Allow location to send or receive updates",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // Geofencing code STARTS HERE
    private GeofencingRequest getGeofencingRequest() {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList).build();
    }

    private ResultCallback<Status> getStatusResult() {
        return new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    geofenceAdded = !geofenceAdded;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(geofencesAddedKey, geofenceAdded);
                    editor.apply();
                } else Log.e("MAPS", status.getStatusCode() + ": " + status.getStatusMessage());
            }
        };
    }

    private void addGeofence() {
        if (!gClient.isConnected())
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        else {
            try {
                LocationServices.GeofencingApi
                        .addGeofences(gClient, getGeofencingRequest(), getGeofencePendingIntent())
                        .setResultCallback(statusResult);
            } catch (SecurityException e) {
                Log.e("MAPS", e.getMessage());
            }
        }
    }

    private void removeGeofence() {
        if (!gClient.isConnected())
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        else {
            try {
                LocationServices.GeofencingApi.removeGeofences(gClient, getGeofencePendingIntent())
                        .setResultCallback(statusResult);
            } catch (SecurityException e) {
                Log.e("MAPS", e.getMessage());
            }
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        return PendingIntent.getService(this, 0, new Intent(this, GeofenceIntentService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createGeofence(Location loc) {
        geofenceList.add(new Geofence.Builder().setRequestId(GEOFENCE_REQUEST_KEY)
                .setCircularRegion(loc.getLatitude(), loc.getLongitude(), geofenceRadius)
                .setExpirationDuration(geofenceExpiration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    // Geofencing code ENDS HERE

    private void startSignInActivity() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void gotoMarker() {
        if (loc != null)
            mMap.animateCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 18));
        else Toast.makeText(this, "Can't find your location", Toast.LENGTH_SHORT).show();
    }

    private void onLocUpdate(LatLng pos) {
        if (marker != null)
            marker.remove();
        if (circle != null)
            circle.remove();
        if (!geofenceList.isEmpty()) {
            geofenceList.remove(0);
            removeGeofence();
        }
        marker = mMap.addMarker(new MarkerOptions().position(pos));
        circle = mMap.addCircle(new CircleOptions().center(pos).radius(geofenceRadius)
                .strokeColor(Color.GREEN).fillColor(Color.alpha(0)));
        createGeofence(loc);
        addGeofence();
    }

    private void addNearbyUserMarkers(User user) {
        if (inRange(user.getLongitude(), user.getLatitude())) {
            userMarkers.add(mMap.addMarker(new MarkerOptions().position(
                    new LatLng(user.getLatitude(), user.getLongitude()))));
            Log.i("MAPS", "user added");
        }
    }

    private void attachUsersListener() {
        if (loc != null) {
            removeUserMarkers();
            if (usersLocationListener == null) {
                usersLocationListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        addNearbyUserMarkers(dataSnapshot.getValue(User.class));
                        Log.i("MAPS", "OnChildAdded");
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        addNearbyUserMarkers(dataSnapshot.getValue(User.class));
                        Log.i("MAPS", "OnChildChanged");
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("MAPS", databaseError.getCode() + " " + databaseError.getMessage());
                    }
                };
                dbUsersRef.addChildEventListener(usersLocationListener);
            }
        }
    }

    private void detachUsersListener() {
        if (usersLocationListener != null) {
            dbUsersRef.removeEventListener(usersLocationListener);
            usersLocationListener = null;
        }
    }

    private boolean inRange(double lng, double lat) {
        if(lng > 90 && lat > 180)
            return false;
        if (lat == loc.getLatitude() && lng == loc.getLongitude())
            return false;
        float[] distance = new float[1];
        Location.distanceBetween(lat, lng, loc.getLatitude(), loc.getLongitude(), distance);
        return distance[0] <= (float) geofenceRadius;
    }

    private void removeUserMarkers() {
        if (userMarkers.size() > 0) {
            for (int i = 0; i < userMarkers.size(); i++) {
                userMarkers.get(i).remove();
                Log.i("MAPS", "User removed");
            }
            userMarkers.clear();
        }
    }

    /*private double pointValue(double r, double p1, double p2) {
        return (((1 - r) * p1) + (r * p2));
    }

    // Check if time has expired of a given time
    private boolean inTimeLength(long timestamp) {
        long fiveMin = 5 * 60 * 1000, beforeFiveMin = System.currentTimeMillis() - fiveMin;
        return (beforeFiveMin <= timestamp);
    }*/

    // Checks location setting and sends back the result
    protected void checkLocSettings() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "asking permission");
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, CHECK_SETTINGS);
        } else {
            Log.i(TAG, "checking gps");
            PendingResult<LocationSettingsResult> res = LocationServices.SettingsApi
                    .checkLocationSettings(gClient, locSettingReq);
            res.setResultCallback(this);
        }
    }

    private void animateBtnAddContent() {
        if (isBtnTapped) {
            isBtnTapped = false;
            btnAddContent.startAnimation(animRotateBackward);
            btnCamera.startAnimation(animBtnClose);
            btnChat.startAnimation(animBtnClose);
            btnVoiceRecord.startAnimation(animBtnClose);
            btnCamera.setClickable(false);
            btnChat.setClickable(false);
            btnVoiceRecord.setClickable(false);
            Log.i("MAPS", "Closed");
        } else {
            isBtnTapped = true;
            btnAddContent.startAnimation(animRotateForward);
            btnCamera.startAnimation(animBtnOpen);
            btnChat.startAnimation(animBtnOpen);
            btnVoiceRecord.startAnimation(animBtnOpen);
            btnCamera.setClickable(true);
            btnChat.setClickable(true);
            btnVoiceRecord.setClickable(true);
            Log.i("MAPS", "Opened");
        }
    }

    private void setLocationBroacastReceiver() {
        locationBroadcastReceiver = new LocationBroadcastReceiver();
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningService:
                am.getRunningServices(Integer.MAX_VALUE)) {
            return BackgroundLocationService.class.getName()
                    .equals(runningService.service.getClassName());
        }
        return false;
    }

    private void startBackgroundService() {
        if (!isServiceRunning()) {
            Intent i = new Intent(this, BackgroundLocationService.class);
            startService(i);
        }
    }

    private void setInstance() {
        if (instance == null) {
            instance = LocationComponentsSingleton.getInstance(this);
            gClient = instance.getGoogleApiClient();
            locSettingReq = instance.getLocationSettingsRequest();
        }
    }

    private void sendLocationUpdateSignal(int priority) {
        Log.i(TAG, "sending location signal");
        Intent i = new Intent(ACTION);
        i.putExtra("startlocationupdate", true);
        i.putExtra("priority", priority);
        localBroadcastManager.sendBroadcast(i);
    }

    private ValueEventListener checkConnectivity() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Boolean.class))
                    isConnected = true;
                else isConnected = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    private void addConnectionListener() {
        if (connectionListener == null)
            connectionListener = checkConnectivity();
        if (conRef == null)
            conRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        conRef.addValueEventListener(connectionListener);
    }

    private void removeConnectionListener() {
        if (connectionListener != null) {
            conRef.removeEventListener(connectionListener);
            connectionListener = null;
        }
    }

    private void getInstances() {
        if (isConnected) {
            DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
            dbUsersRef = dbRootRef.child("users");
            userAuth = FirebaseAuth.getInstance();
        }
    }

    private FirebaseAuth.AuthStateListener getUserVerified() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null)
                    startSignInActivity();
            }
        };
    }


    private final class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean client = intent.getBooleanExtra("client", false),
                    request = intent.getBooleanExtra("request", false);
            if (client && request) {
                Log.i(TAG, "checking for permission");
                checkLocSettings();
            }
            if (intent.getExtras().get("location") != null) {
                loc = (Location) intent.getExtras().get("location");
                onLocUpdate(new LatLng(loc.getLatitude(), loc.getLongitude()));
            }
        }
    }
}