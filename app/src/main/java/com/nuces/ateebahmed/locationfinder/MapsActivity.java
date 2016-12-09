package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import models.Message;
import models.User;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener,
        ResultCallback<LocationSettingsResult> {

    private GoogleMap mMap;
    protected GoogleApiClient gClient;
    protected LocationRequest locReq;
    protected LocationSettingsRequest locSettingReq;
    protected Location loc;
    protected boolean locUpd;
    protected static final int CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL = 30000;
    private static final long FASTEST_UPDATE = 5000;
    protected ArrayList<Geofence> geofenceList;
    private boolean geofenceAdded;
    private SharedPreferences sharedPreferences;
    private ResultCallback<Status> statusResult;
    protected Marker marker;
    protected Circle circle;
    private UserSession session;
    private DatabaseReference dbRootRef, dbUsersRef, dbMessagesRef, dbUser;
    private Button btnSearchLocation, btnGotoMarker, btnSend;
    private ArrayList<Marker> userMarkers;
    private TextView txtChat;
    private EditText etMsgSpace;

    // Constants
    private String packageName = "com.nuces.ateebahmed.locationfinder",
            sharedPreferencesName = packageName + ".SHARED_PREFERENCES_NAME",
            geofencesAddedKey = packageName + ".GEOFENCES_ADDED_KEY";
    private long geofenceExpiration = 60 * 60 * 1000, geofenceRadius = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSearchLocation = (Button) findViewById(R.id.btnSearchLocation);
        btnGotoMarker = (Button) findViewById(R.id.btnGotoMarker);
        btnGotoMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMarker();
            }
        });

        txtChat = (TextView) findViewById(R.id.txtChat);
        etMsgSpace = (EditText) findViewById(R.id.etMsgSpace);

        session = new UserSession(getApplicationContext());
        if (!session.isLoggedIn()) {
            startSignInActivity();
        }

        geofenceList = new ArrayList<>();
        userMarkers = new ArrayList<>();
        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        geofenceAdded = sharedPreferences.getBoolean(geofencesAddedKey, false);
        statusResult = getStatusResult();
        locUpd = false;
        marker = null;
        circle = null;

        dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");
        dbMessagesRef = dbRootRef.child("messages");
        if (session.isLoggedIn())
            dbUser = dbUsersRef.child(session.getDbKey()).getRef();

        clientBuilder();
        createLocReq();
        createLocSettingReq();
        checkLocSettings();

        btnSearchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gClient.isConnected())
                    startLocUpds();
                if (locUpd)
                    gotoMarker();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMessageInDatabase();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        addNearbyUsers();
        getNearbyMessages();
    }

    // Google Play Services client initialized
    protected synchronized void clientBuilder() {
        gClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    // Creating a location request for detecting location providing parameters
    protected void createLocReq() {
        locReq = new LocationRequest();
        locReq.setInterval(UPDATE_INTERVAL);
        locReq.setFastestInterval(FASTEST_UPDATE);
        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void createLocSettingReq() {
        LocationSettingsRequest.Builder b = new LocationSettingsRequest.Builder();
        b.addLocationRequest(locReq);
        locSettingReq = b.build();
    }

    protected void checkLocSettings() {
        PendingResult<LocationSettingsResult> res = LocationServices.SettingsApi
                .checkLocationSettings(gClient, locSettingReq);
        res.setResultCallback(this);
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
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MAPS", "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        gClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("MAPS", connectionResult.getErrorCode() + ": " + connectionResult.getErrorMessage());
    }

    // Location detection service functions START HERE


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
    }

    @Override
    protected void onStart() {
        super.onStart();
        gClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeGeofence();
        if (gClient.isConnected())
            gClient.disconnect();
        userMarkers.clear();
        removeLocationFromDatabase();
    }

    private void startLocUpds() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
            }, 1);
        } else {
            Toast.makeText(this, "Getting your location", Toast.LENGTH_SHORT).show();
            LocationServices.FusedLocationApi.requestLocationUpdates(gClient, locReq, this)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            locUpd = true;
                        }
                    });
        }
    }

    private void stopLocUpds() {
        if (locUpd)
            LocationServices.FusedLocationApi.removeLocationUpdates(gClient, this)
                    .setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    locUpd = false;
                }
            });
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeGeofence();
        stopLocUpds();
        userMarkers.clear();
        removeLocationFromDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (gClient.isConnected() && locUpd) {
            startLocUpds();
            addGeofence();
        }*/
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                if (!gClient.isConnected())
                    gClient.connect();
//                startLocUpds();
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
                startLocUpds();
                Log.i("MAPS", "Okay");
                break;
            case Activity.RESULT_CANCELED:
                Log.e("MAPS", "cancelled");
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Enable location in Settings", Toast.LENGTH_LONG).show();
        }
    }
    // Location detection service functions END HERE

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
                    Toast.makeText(MapsActivity.this, geofenceAdded ? "Geofence created" :
                            "Geofence not created",
                            Toast.LENGTH_SHORT).show();
                } else Log.e("MAPS", status.getStatusMessage());
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
                        Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(1).build());
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
        addLocationToDatabase();
        createGeofence(loc);
        addGeofence();
    }

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
        float[] distance = new float[1];
        Location.distanceBetween(lat, lng, loc.getLatitude(), loc.getLongitude(), distance);
        return distance[0] <= (float) geofenceRadius;
    }

    private void removeUserMarkers() {
        if (userMarkers.size() > 0)
            for (int i = 0; i < userMarkers.size(); i++){
                    userMarkers.get(i).remove();
                    Log.i("MAPS", "User removed");
            }
        userMarkers.clear();
    }

    private double pointValue(double r, double p1, double p2) {
        return (((1 - r) * p1) + (r * p2));
    }

    private void saveMessageInDatabase() {
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
    }

    private boolean inTimeLength(long timestamp) {
        long fiveMin = 5 * 60 * 1000, beforeFiveMin = System.currentTimeMillis() - fiveMin;
        return (beforeFiveMin <= timestamp);
    }

    private void removeLocationFromDatabase() {
        dbUser.child("latitude").setValue(181);
        dbUser.child("longitude").setValue(91);
    }
}
