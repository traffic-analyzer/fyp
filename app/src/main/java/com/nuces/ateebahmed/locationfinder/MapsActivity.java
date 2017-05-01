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
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
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
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.SnappedPoint;
import com.google.maps.model.Unit;

import org.joda.time.Instant;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Request;
import models.RequestedUsers;
import models.User;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        ResultCallback<LocationSettingsResult>, GoogleMap.OnMarkerClickListener {

    protected static final String ACTION = "com.nuces.ateebahmed.locationfinder.MapsActivity";
    private static final String TAG = "MapsActivity";

    private BroadcastReceiver locationBroadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private PlacesAutoCompleteAdapter adapter;
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
    private ArrayList<Marker> userMarkers, messageMarkers;
    protected ArrayList<Geofence> geofenceList;

    private DatabaseReference dbUsersRef, conRef, dbMessagesRef, dbRequestsRef, dbRequestedUsersRef;
    private ValueEventListener connectionListener;
    private ChildEventListener usersLocationListener, messageListener;

    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userVerified;

//    private Toolbar searchBar;
    private FloatingActionButton btnGotoMarker, btnAddContent, btnCamera, btnChat, btnVoiceRecord,
                                    btnAskDirections;
    private Animation animBtnOpen, animBtnClose, animRotateForward, animRotateBackward;
    private AppCompatAutoCompleteTextView tvSearchSrc, tvSearchDest;
    private AppCompatButton btnGo, btnCancel, btnRequestYes, btnRequestNo;

    // Constants
    private String packageName = "com.nuces.ateebahmed.locationfinder",
            sharedPreferencesName = packageName + ".SHARED_PREFERENCES_NAME",
            geofencesAddedKey = packageName + ".GEOFENCES_ADDED_KEY";
    private long geofenceExpiration = 60 * 60 * 1000, geofenceRadius = 100;
    protected static final int CHECK_SETTINGS = 0x1, ENABLE_LOCATION = 0x2;
    private static final String GEOFENCE_REQUEST_KEY = "own";
    private String mediaUri, srcPlaceId, destPlaceId, userId;
    private GeoApiContext context;
    private LatLng srcLatLng, destLatLng;
    private Polyline route;
    private BottomSheetBehavior bottomSheetDirectionsBehavior, bottomSheetRequestBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /*searchBar = (Toolbar) findViewById(R.id.searchBar);
        setSupportActionBar(searchBar);*/
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
            }
        });
        btnCamera = (FloatingActionButton) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
                Intent camera = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(camera);
            }
        });
        btnChat = (FloatingActionButton) findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateBtnAddContent();
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
        btnGo = (AppCompatButton) findViewById(R.id.btnGo);
        btnGo.setOnClickListener(getDirections());
        btnCancel = (AppCompatButton) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDirectionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        btnAskDirections = (FloatingActionButton) findViewById(R.id.btnAskDirections);
        btnAskDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDirectionsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        btnRequestYes = (AppCompatButton) findViewById(R.id.btnRequestYes);
        btnRequestYes.setOnClickListener(sendRouteTrafficRequest());
        btnRequestNo = (AppCompatButton) findViewById(R.id.btnRequestNo);
        btnRequestNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetRequestBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        bottomSheetDirectionsBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetDirections));
        bottomSheetDirectionsBehavior.setHideable(true);
        bottomSheetDirectionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetRequestBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetRequest));
        bottomSheetRequestBehavior.setHideable(true);
        bottomSheetRequestBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        tvSearchSrc = (AppCompatAutoCompleteTextView) findViewById(R.id.tvSearchSrc);
        tvSearchDest = (AppCompatAutoCompleteTextView) findViewById(R.id.tvSearchDest);

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

        isConnected = false;

        getInstances();
        setInstance();
        setLocationBroacastReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        geofenceList = new ArrayList<>();
        statusResult = getStatusResult();
        userMarkers = new ArrayList<>();
        messageMarkers = new ArrayList<>();
        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        geofenceAdded = sharedPreferences.getBoolean(geofencesAddedKey, false);
        locUpd = false;
        marker = null;
        circle = null;
        context = new GeoApiContext().setApiKey("AIzaSyBfqbjPyZZacjq5XuJVUw_3RgXbehyJK0c");
        adapter = new PlacesAutoCompleteAdapter(this, gClient);
        tvSearchSrc.setAdapter(adapter);
        tvSearchSrc.setOnItemSelectedListener(getSelectedPlace(tvSearchSrc.getId()));
        tvSearchSrc.setOnItemClickListener(getClickedPlace(tvSearchSrc.getId()));
        tvSearchDest.setAdapter(adapter);
        tvSearchDest.setOnItemSelectedListener(getSelectedPlace(tvSearchDest.getId()));
        tvSearchDest.setOnItemClickListener(getClickedPlace(tvSearchDest.getId()));
        route = null;
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

        if(mMap != null) {
            // Move the camera to Karachi, PK
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(24.8615, 67.0099), 10));
            mMap.setOnMarkerClickListener(this);
        }
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(0, 0);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        addConnectionListener();
        addAuthStateListener();
        attachUsersListener();
        attachMessagesListener();
        localBroadcastManager.registerReceiver(locationBroadcastReceiver,
                new IntentFilter(BackgroundLocationService.ACTION));
        if (!isLocationProviderEnabled())
            openLocationDialogue();
//        startBackgroundService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(locationBroadcastReceiver);
        detachUsersListener();
        detachMessagesListener();
        if (!geofenceList.isEmpty()) {
            geofenceList.remove(0);
            removeGeofence();
        }
        removeUserMarkers();
        removeAuthStateListener();
        removeConnectionListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendLocationUpdateSignal(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        removeAuthStateListener();
        removeConnectionListener();
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
                signalServiceToStop();
                Log.e(TAG, "Location settings cannot be done");
                Toast.makeText(this, "Enable Location in Setttings", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                    sendLocationUpdateSignal(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    Log.i(TAG, "activity result okay");
                break;
            case Activity.RESULT_CANCELED:
                    signalServiceToStop();
                    Log.e(TAG, "activity result cancelled");
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
                    signalServiceToStop();
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
            openLocationDialogue();
        }
    }

    private void openLocationDialogue() {
        if (instance.getLocationRequest().getPriority() != LocationRequest.PRIORITY_HIGH_ACCURACY)
            instance.setLocationPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        PendingResult<LocationSettingsResult> res = LocationServices.SettingsApi
                .checkLocationSettings(gClient, locSettingReq);
        res.setResultCallback(this);
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
        locationBroadcastReceiver = receiveServiceSignals();
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
            // only for when background service is disabled
            gClient.connect();
            locSettingReq = instance.getLocationSettingsRequest();
        }
        instance.setLocationPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void sendLocationUpdateSignal(int priority) {
        Log.i(TAG, "sending location signal");
        Intent i = new Intent(ACTION);
        i.putExtra("startlocationupdate", true);
        i.putExtra("priority", priority);
        localBroadcastManager.sendBroadcast(i);
    }

    private void signalServiceToStop() {
        Intent i = new Intent(ACTION);
        i.putExtra("stop", true);
        localBroadcastManager.sendBroadcast(i);
    }

    private ValueEventListener checkConnectivity() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isConnected = dataSnapshot.getValue(Boolean.class);
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

    private void addAuthStateListener() {
        if (userVerified == null)
            userVerified = getUserVerified();
        userAuth.addAuthStateListener(userVerified);
        Log.i(TAG, "auth listener added");
    }

    private void removeAuthStateListener() {
        if (userVerified != null) {
            userAuth.removeAuthStateListener(userVerified);
            userVerified = null;
            Log.i(TAG, "auth listener removed");
        }
    }

    private void getInstances() {
        DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
        dbUsersRef = dbRootRef.child("users");
        dbMessagesRef = dbRootRef.child("messages");
        dbRequestsRef = dbRootRef.child("requests");
        dbRequestedUsersRef = dbRootRef.child("requested_users");
        userAuth = FirebaseAuth.getInstance();
        Log.i(TAG, "got instances");
    }

    private FirebaseAuth.AuthStateListener getUserVerified() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.i(TAG, "checking user");
                if (firebaseAuth.getCurrentUser() == null)
                    startSignInActivity();
                else userId = firebaseAuth.getCurrentUser().getUid();
            }
        };
    }

    private boolean isLocationProviderEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void setLocation(Location location) {
        loc = location;
    }

    private BroadcastReceiver receiveServiceSignals() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean request = intent.getBooleanExtra("request", false);
                if (request) {
                    Log.i(TAG, "checking for permission");
                    checkLocSettings();
                }
                if (intent.getExtras().get("location") != null) {
                    setLocation((Location) intent.getExtras().get("location"));
                    onLocUpdate(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
            }
        };
    }

    private void attachMessagesListener() {
        if(messageListener == null) {
            messageListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    addMessageMarkers(dataSnapshot);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };

            dbMessagesRef.addChildEventListener(messageListener);
        }
    }

    private void detachMessagesListener() {
        if (messageListener != null) {
            dbMessagesRef.removeEventListener(messageListener);
            messageListener = null;
        }
    }

    private void addMessageMarkers(DataSnapshot dataSnapshot) {
        Date date = new Date(dataSnapshot.child("timestamp").getValue(Long.class));
        messageMarkers.add(mMap.addMarker(new MarkerOptions()
                .position(new LatLng((Double) dataSnapshot.child("latitude").getValue(),
                        (Double) dataSnapshot.child("longitude").getValue()))
                .title(new SimpleDateFormat("d MMM yyyy HH:mm a", Locale.getDefault()).format(date))
                .icon(setMarkerIcon(dataSnapshot))
                .snippet(String.valueOf(dataSnapshot.getKey()))));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        if (isConnected)
            dbMessagesRef.child(marker.getSnippet())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("audio") || dataSnapshot.hasChild("image") ||
                                dataSnapshot.hasChild("video"))
                            getMediaUri(dataSnapshot);
                        else mediaUri = "";
                        showBottomSheet(dataSnapshot.hasChild("message")
                                ? dataSnapshot.child("message").getValue(String.class) : "");
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        else Toast.makeText(MapsActivity.this, "No internet connection available",
                Toast.LENGTH_SHORT).show();
        return true;
    }

    private BitmapDescriptor setMarkerIcon(DataSnapshot dataSnapshot) {
        if (dataSnapshot.hasChild("image"))
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_camera);
        else if(dataSnapshot.hasChild("audio"))
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_mic);
        else if (dataSnapshot.hasChild("video"))
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_video);
        else return BitmapDescriptorFactory.fromResource(R.drawable.ic_message);
    }

    private void getMediaUri(DataSnapshot snapshot) {
        if (snapshot.hasChild("image"))
            mediaUri = snapshot.child("image").getValue(String.class);
        else if (snapshot.hasChild("video"))
            mediaUri = snapshot.child("video").getValue(String.class);
        else if (snapshot.hasChild("audio"))
            mediaUri = snapshot.child("audio").getValue(String.class);
    }

    private void showBottomSheet(String message) {
        MarkerDetailsBottomSheet markerDetails;
        if (!mediaUri.isEmpty())
            markerDetails = MarkerDetailsBottomSheet
                .newInstance(mediaUri);
        else markerDetails = MarkerDetailsBottomSheet.newInstance(message);
        markerDetails.show(getSupportFragmentManager(), markerDetails.getTag());
    }

    private AdapterView.OnItemSelectedListener getSelectedPlace(final int id) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setPlaceIds(adapterView, id, i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                setPlaceIds(adapterView, id, 0);
            }
        };
    }

    private AdapterView.OnItemClickListener getClickedPlace(final int id) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                setPlaceIds(adapterView, id, i);
            }
        };
    }

    private void setPlaceIds(AdapterView<?> adapterView, int id, int i) {
        switch (id) {
            case R.id.tvSearchSrc:
                srcPlaceId = ((AutocompletePrediction)adapterView.getItemAtPosition(i)).getPlaceId();
                break;
            case R.id.tvSearchDest:
                destPlaceId = ((AutocompletePrediction)adapterView.getItemAtPosition(i)).getPlaceId();
                break;
            default:
                break;
        }
    }

    private View.OnClickListener getDirections() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                if (srcPlaceId == null || destPlaceId == null)
                    return;
                if (!isConnected) {
                    Toast.makeText(MapsActivity.this, "No internet connection available",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // for getting LatLng
                Places.GeoDataApi.getPlaceById(gClient, srcPlaceId)
                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (places.getStatus().isSuccess() && places.getCount() > 0) {
                                    final Place myPlace = places.get(0);
                                    srcLatLng = myPlace.getLatLng();
                                    getDirectionsResult();
                                }
                                places.release();
                            }
                        });
                Places.GeoDataApi.getPlaceById(gClient, destPlaceId)
                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (places.getStatus().isSuccess() && places.getCount() > 0) {
                                    final Place myPlace = places.get(0);
                                    destLatLng = myPlace.getLatLng();
                                    getDirectionsResult();
                                }
                                places.release();
                            }
                        });
                bottomSheetDirectionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        };
    }

    private com.google.maps.model.LatLng getLatLngForDirections(LatLng ll) {
        return new com.google.maps.model.LatLng(ll.latitude, ll.longitude);
    }

    private void getDirectionsResult() {

        if (srcLatLng != null && !srcLatLng.toString().trim().isEmpty() && destLatLng != null &&
                !destLatLng.toString().trim().isEmpty() && isConnected) {
            try {

                DirectionsResult result = DirectionsApi.newRequest(context)
                        .origin(getLatLngForDirections(srcLatLng))
                        .destination(getLatLngForDirections(destLatLng))
                        .departureTime(Instant.now()).optimizeWaypoints(true).units(Unit.METRIC)
                        .await();

                ArrayList<SnappedPoint[]> path = getSmoothRoute(result.routes[0].overviewPolyline
                                                                .decodePath());

                ArrayList<LatLng> latLngs = convertLatLng(path);
                PolylineOptions options = new PolylineOptions();
                options.color(Color.BLACK);
                options.width(10);
                for (LatLng l : latLngs) {
                    options.add(l);
                }

                if (route != null)
                    route.remove();
                route = mMap.addPolyline(options);

                bottomSheetRequestBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } catch (ApiException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        } else if (!isConnected)
            Toast.makeText(MapsActivity.this, "No internet connection available",
                    Toast.LENGTH_SHORT).show();
    }

    private ArrayList<SnappedPoint[]> getSmoothRoute(List<com.google.maps.model.LatLng> route) {

        ArrayList<SnappedPoint[]> snapped = new ArrayList<>();
        com.google.maps.model.LatLng pointsArray[];
        com.google.maps.PendingResult<SnappedPoint[]> pointResult;

        for (int i = 0; i < route.size(); i += 100) {
            pointsArray = new com.google.maps.model
                    .LatLng[(Math.min(i + 100, route.size() - 1) % 101)];
            for (int j = 0; j < pointsArray.length; ++j)
                pointsArray[j] = route.get(i + j);

            pointResult = RoadsApi.snapToRoads(context, true, pointsArray);

            try {
                snapped.add(pointResult.await());
            } catch (ApiException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
        return snapped;
    }

    private ArrayList<LatLng> convertLatLng(ArrayList<SnappedPoint[]> snapped) {

        ArrayList<LatLng> smoothCoords = new ArrayList<>();
        double latlng[] = new double[2];

        for (int i = 0; i < snapped.size(); ++i)
            for (int j = 0; j < snapped.get(i).length; ++j) {
                String points[] = snapped.get(i)[j].location.toString().split(",");
                latlng[0] = Double.parseDouble(points[0]);
                latlng[1] = Double.parseDouble(points[1]);
                smoothCoords.add(new LatLng(latlng[0], latlng[1]));
            }

        return smoothCoords;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context
                                                                            .INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private View.OnClickListener sendRouteTrafficRequest() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (route != null && isConnected) {
                    Request r = new Request(route.getPoints(), userId, System.currentTimeMillis());
                    DatabaseReference requestId = dbRequestsRef.push();
                    requestId.setValue(r).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MapsActivity.this,
                                    "Wait a bit, results will be updated on map",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MapsActivity.this,
                                    "There was an error when sending your request",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    setRequestedUsers(requestId.getKey());
                } else if (!isConnected)
                    Toast.makeText(MapsActivity.this, "No internet connection available",
                            Toast.LENGTH_SHORT).show();
                bottomSheetRequestBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        };
    }

    private void setRequestedUsers(final String requestId) {
        dbUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<User> users =
                        new ArrayList<>();
                for (DataSnapshot snapshot: dataSnapshot.getChildren())
                    if (snapshot.hasChild("token"))
                        users.add(snapshot.getValue(User.class));
                LatLng ll;
                RequestedUsers reqUsers = new RequestedUsers(requestId, new ArrayList<String>());
                for (int i = 0; i < users.size(); ++i) {
                    ll = new LatLng(users.get(i).getLatitude(),
                            users.get(i).getLongitude());
                    if(PolyUtil.isLocationOnPath(ll, route.getPoints(), true, 7))
                        reqUsers.getUsersTokens().add(users.get(i).getToken());
                }
                dbRequestedUsersRef.push().setValue(reqUsers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}