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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import android.widget.Button;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import models.User;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        /*ConnectionCallbacks, OnConnectionFailedListener, LocationListener,*/
        ResultCallback<LocationSettingsResult> {

    protected static final String ACTION = "com.nuces.ateebahmed.locationfinder.MapsActivity";
    private static final String TAG = "MapsActivity";

    private BroadcastReceiver locationBroacastReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private LocationComponentsSingleton instance;
    protected GoogleApiClient gClient;
    protected LocationSettingsRequest locSettingReq;
    protected Location loc;
    protected boolean locUpd, geofenceAdded, isBtnTapped;
    private SharedPreferences sharedPreferences;
    private ResultCallback<Status> statusResult;

    private GoogleMap mMap;
    protected Marker marker;
    protected Circle circle;
    private ArrayList<Marker> userMarkers;
    protected ArrayList<Geofence> geofenceList;

    private UserSession session;
    private DatabaseReference dbUsersRef, dbMessagesRef, dbUser;
//    private ChildEventListener usersLocationListener, messageListener;

    private Button btnSearchLocation;
    /*private TextView txtChat;
    */
    private Toolbar searchBar;
    private FloatingActionButton btnGotoMarker, btnAddContent, btnCamera, btnChat;
    private Animation animBtnOpen, animBtnClose, animRotateForward, animRotateBackward;
    private String imagePath;

    // Constants
    private String packageName = "com.nuces.ateebahmed.locationfinder",
            sharedPreferencesName = packageName + ".SHARED_PREFERENCES_NAME",
            geofencesAddedKey = packageName + ".GEOFENCES_ADDED_KEY";
    private long geofenceExpiration = 60 * 60 * 1000, geofenceRadius = 100;
    private static final int REQ_IMAGE_CAPTURE = 1;
    protected static final int CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL = 30000, FASTEST_UPDATE = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setInstance();

        searchBar = (Toolbar) findViewById(R.id.searchBar);
        setSupportActionBar(searchBar);
        /*
        btnSearchLocation = (Button) findViewById(R.id.btnSearchLocation);
        */
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
                startCamera();
//                addImageToGallery();
            }
        });
        btnChat = (FloatingActionButton) findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
                Log.i("MAPS", "Chat");
                Intent chat = new Intent(getApplicationContext(), ChatActivity.class);
                startActivity(chat);
            }
        });

        animBtnOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_open);
        animBtnClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_close);
        animRotateForward = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_forward);
        animRotateBackward = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_backward);
        /*txtChat = (TextView) findViewById(R.id.txtChat);
        */

        session = new UserSession(getApplicationContext());
        if (!session.isLoggedIn()) {
            startSignInActivity();
        }

        geofenceList = new ArrayList<>();
        userMarkers = new ArrayList<>();
        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        geofenceAdded = sharedPreferences.getBoolean(geofencesAddedKey, false);
        locUpd = false;
        marker = null;
        circle = null;

        /*DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");
        dbMessagesRef = dbRootRef.child("messages");
        if (session.isLoggedIn())
            dbUser = dbUsersRef.child(session.getDbKey()).getRef();*/

        /*btnSearchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gClient.isConnected())
                    startLocUpds();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMessageInDatabase();
            }
        });*/

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setLocationBroacastReceiver();

        startBackgroundService();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

    /*@Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MAPS", "connected");
        *//*createLocReq();
        createLocSettingReq();
        checkLocSettings();*//*
        statusResult = getStatusResult();
        *//*attachUsersListener();
        attachMessageListener();
        addNearbyUsers();
        getNearbyMessages();*//*
    }

    @Override
    public void onConnectionSuspended(int i) {
        gClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("MAPS", connectionResult.getErrorCode() + ": " + connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {

        loc = location;
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        onLocUpdate(pos);
        if (geofenceList.size() > 0) {
            geofenceList.remove(0);
            removeGeofence();
        }
        Log.i("MAPS", "changed");
    }*/

    @Override
    protected void onStart() {
        super.onStart();
//        gClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        detachMessageListener();
//        detachUsersListener();
        removeGeofence();
        /*removeUserMarkers();
        removeLocationFromDatabase();
        if (gClient.isConnected())
            gClient.disconnect();*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendLocationUpdateSignal(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        localBroadcastManager.unregisterReceiver(locationBroacastReceiver);
//        detachMessageListener();
//        detachUsersListener();
        removeGeofence();
        /*stopLocUpds();
        removeUserMarkers();
        removeLocationFromDatabase();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (gClient.isConnected() && locUpd) {
            startLocUpds();
            attachMessageListener();
            attachUsersListener();
        }*/
        IntentFilter filter = new IntentFilter(BackgroundLocationService.ACTION);
        localBroadcastManager.registerReceiver(locationBroacastReceiver, filter);
        checkLocSettings();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                /*if (!gClient.isConnected())
                    gClient.connect();
                startLocUpds();*/
                sendLocationUpdateSignal(LocationRequest.PRIORITY_HIGH_ACCURACY);
                Toast.makeText(this, "Location enabled", Toast.LENGTH_SHORT).show();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                try {
                    status.startResolutionForResult(MapsActivity.this, CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("MAPS", e.getMessage());
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e("MAPS", "Location settings cannot be done");
                Toast.makeText(this, "Enable Location in Setttings", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Activity.RESULT_OK:
                sendLocationUpdateSignal(LocationRequest.PRIORITY_HIGH_ACCURACY);
//                startLocUpds();
                Log.i("MAPS", "Okay");
                break;
            case Activity.RESULT_CANCELED:
                Log.e("MAPS", "cancelled");
                break;
            case REQ_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    addImageToGallery();
//                    Bundle extras = data.getExtras();
//                    Bitmap image = (Bitmap) extras.get("data");
//                    // TODO: create an imageview
//                    imageView.setImageBitmap(image);
                    Log.i("MAPS", "captured");
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Enable location in Settings", Toast.LENGTH_LONG).show();
                /*else startLocUpds();*/
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

    // Geofencing code STARTS HERE
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder b = new GeofencingRequest.Builder();
        b.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        b.addGeofences(geofenceList);
        return b.build();
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
                    if (geofenceAdded)
                    Toast.makeText(MapsActivity.this, "Geofence created", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(MapsActivity.this, "Geofence not created",
                            Toast.LENGTH_SHORT).show();
                } else Log.e("MAPS", status.getStatusCode() + status.getStatusMessage());
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
        Intent intent = new Intent(this, GeofenceIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createGeofence(Location loc) {
        geofenceList.add(new Geofence.Builder().setRequestId("Test")
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

    private void addLocationToDatabase() {
        dbUser.child("latitude").setValue(loc.getLatitude());
        dbUser.child("longitude").setValue(loc.getLongitude());
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
        marker = mMap.addMarker(new MarkerOptions().position(pos));
        circle = mMap.addCircle(new CircleOptions().center(pos).radius(geofenceRadius)
                .strokeColor(Color.GREEN).fillColor(Color.alpha(0)));
//        addLocationToDatabase();
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

    /*private void attachUsersListener() {
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
    }*/

    private void addNearbyUsers() {
        dbUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                removeUserMarkers();
                if (!dataSnapshot.exists()) {
                    Log.e("MAPS", "users object not found");
                    return;
                }
                if (loc != null)
                    for (DataSnapshot ids : dataSnapshot.getChildren()) {
                        if (!ids.getKey().equals(dbUser.getKey())) {
                            User user = ids.getValue(User.class);
                            if (inRange(user.getLongitude(), user.getLatitude())) {
                                userMarkers.add(mMap.addMarker(new MarkerOptions().position(
                                        new LatLng(user.getLatitude(), user.getLongitude()))));
                                Log.i("MAPS", "User added");
                            }
                        }
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MAPS", databaseError.getMessage());
            }
        });
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

    private double pointValue(double r, double p1, double p2) {
        return (((1 - r) * p1) + (r * p2));
    }

    /*private void saveMessageInDatabase() {
        if (loc == null) {
            Toast.makeText(this, "Enable location first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etMsgSpace.getText().toString().isEmpty()) {
            Toast.makeText(this, "Write something", Toast.LENGTH_SHORT).show();
            return;
        }
        Message msg = new Message(etMsgSpace.getText().toString().trim(), session.getSPUsername(),
                loc.getLongitude(), loc.getLatitude(), System.currentTimeMillis());
        dbMessagesRef.push().setValue(msg);
        Log.i("MAPS", "Message sent");
    }

    private String addNearbyMessages(Message m) {
        if (inTimeLength(m.getTimestamp())) {
            if (inRange(m.getLongitude(), m.getLatitude())) {
                return DateFormat.getTimeFormat(getApplicationContext())
                        .format(m.getTimestamp()) + "\n"
                        + m.getUsername() +
                        ": " + m.getMessage().trim() + "\n";
            }
        }
        return "";
    }

    private void attachMessageListener() {
        if (messageListener == null) {
            messageListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    txtChat.append(addNearbyMessages(dataSnapshot.getValue(Message.class)));
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            dbMessagesRef.addChildEventListener(messageListener);
        }
    }

    private void detachMessageListener() {
        if (messageListener != null) {
            dbMessagesRef.removeEventListener(messageListener);
            messageListener = null;
        }
    }

    // Fetches messages of Geofenced users
    private void getNearbyMessages() {
        dbMessagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (loc == null) {
                    Toast.makeText(MapsActivity.this, "Enable location first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (geofenceList.isEmpty())
                    return;
                if (userMarkers.isEmpty()) {
                    Toast.makeText(MapsActivity.this, "No users present nearby", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dataSnapshot.exists()) {
                    String line = "";
                    for (DataSnapshot ids: dataSnapshot.getChildren()) {
                        if (inTimeLength(ids.child("timestamp").getValue(Long.class))) {
                            if (inRange(ids.child("longitude").getValue(Double.class),
                                    ids.child("latitude").getValue(Double.class))) {
                                Message msg = ids.getValue(Message.class);
                                line += DateFormat.getTimeFormat(getApplicationContext())
                                                        .format(msg.getTimestamp()) + "\n"
                                        + msg.getUsername() +
                                        ": " + msg.getMessage() + "\n";
                                Log.i("MAPS", "Message received");
                            }
                        }
                    }
                    txtChat.setText(line);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MAPS", databaseError.getCode() + ": " + databaseError.getMessage());
            }
        });
    }*/

    // Check if time has expired of a given time
    private boolean inTimeLength(long timestamp) {
        long fiveMin = 5 * 60 * 1000, beforeFiveMin = System.currentTimeMillis() - fiveMin;
        return (beforeFiveMin <= timestamp);
    }

    // Updates user's location so it won't show on map
    private void removeLocationFromDatabase() {
        dbUser.child("latitude").setValue(181);
        dbUser.child("longitude").setValue(91);
    }

    // Google Play Services client initialized
    /*protected synchronized void clientBuilder() {
        gClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }*/

    // Creating a location request for detecting location providing parameters
    /*protected void createLocReq() {
        locReq = new LocationRequest();
        locReq.setInterval(UPDATE_INTERVAL);
        locReq.setFastestInterval(FASTEST_UPDATE);
        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }*/

    // Create a request dialgoue if location is off
    /*protected void createLocSettingReq() {
        LocationSettingsRequest.Builder b = new LocationSettingsRequest.Builder();
        b.addLocationRequest(locReq);
        locSettingReq = b.build();
    }*/

    // Checks location setting and sends back the result
    protected void checkLocSettings() {
        PendingResult<LocationSettingsResult> res = LocationServices.SettingsApi
                .checkLocationSettings(gClient, locSettingReq);
        res.setResultCallback(this);
    }

    // Checks for allowed permissions
    private boolean checkLocationPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    // Requests for desired location if not allowed
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
        }, 1);
    }

    // Starts location service to detect location
    /*private void startLocUpds() {
        if (checkLocationPermission()) {
            Toast.makeText(this, "Getting your location", Toast.LENGTH_SHORT).show();
            LocationServices.FusedLocationApi.requestLocationUpdates(gClient, locReq, this)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                locUpd = true;
                                gotoMarker();
//                            attachMessageListener();
                                attachUsersListener();
                            } else {
                                Log.e("MAPS", status.getStatusCode() + status.getStatusMessage());
                            }
                        }
                    });
        } else requestLocationPermission();
    }*/

    // Stops location service
    /*private void stopLocUpds() {
        if (locUpd)
            LocationServices.FusedLocationApi.removeLocationUpdates(gClient, this)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess())
                                locUpd = false;
                            else Log.e("MAPS", status.getStatusCode() + status.getStatusMessage());
                        }
                    });
    }*/

    private void animateBtnAddContent() {
        if (isBtnTapped) {
            isBtnTapped = false;
            btnAddContent.startAnimation(animRotateBackward);
            btnCamera.startAnimation(animBtnClose);
            btnChat.startAnimation(animBtnClose);
            btnCamera.setClickable(false);
            btnChat.setClickable(false);
            Log.i("MAPS", "Closed");
        } else {
            isBtnTapped = true;
            btnAddContent.startAnimation(animRotateForward);
            btnCamera.startAnimation(animBtnOpen);
            btnChat.startAnimation(animBtnOpen);
            btnCamera.setClickable(true);
            btnChat.setClickable(true);
            Log.i("MAPS", "Opened");
        }
    }

    private void startCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) != null) {
            File imageFile = null;
            imageFile = createImageFile();
            if (imageFile != null) {
                Uri imageUri = FileProvider.getUriForFile(this, packageName, imageFile);
                camera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(camera, REQ_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TA_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            imagePath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
        // Save a file: path for use with ACTION_VIEW intents
    }

    private void addImageToGallery() {
        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri uri = Uri.fromFile(f);
        media.setData(uri);
        this.sendBroadcast(media);
    }

    private void setLocationBroacastReceiver() {
        locationBroacastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean client = intent.getBooleanExtra("client", false),
                        request = intent.getBooleanExtra("request", false);
                if (client && request) {
                    Log.i(TAG, "checking for permission");
                    checkLocSettings();
//                    checkLocationPermissions();
                }
                if (intent.getExtras().get("location") != null) {
                    loc = (Location) intent.getExtras().get("location");
                    onLocUpdate(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
            }
        };
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
        Log.i(TAG, isServiceRunning() + "");
        if (!isServiceRunning()) {
            Intent i = new Intent(this, BackgroundLocationService.class);
            i.putExtra("message", "gimme location");
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
}