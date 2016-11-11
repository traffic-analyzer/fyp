package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener,
        ResultCallback<LocationSettingsResult>, OnMapClickListener {

    private GoogleMap mMap;
    protected GoogleApiClient gClient;
    protected LocationRequest locReq;
    protected LocationSettingsRequest locSettingReq;
    protected Location loc;
    protected boolean locUpd;
    protected static final int CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL = 30000;
    private static final long FASTEST_UPDATE = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locUpd = false;
        clientBuilder();
        createLocReq();
        createLocSettingReq();
        checkLocSettings();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    protected synchronized void clientBuilder() {
        gClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

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
        mMap.setIndoorEnabled(false);

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(0, 0);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (locUpd)
            startLocUpds();
        Log.i("MAPS", "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        gClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("MAPS", connectionResult.getErrorCode() + "");
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(pos));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
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
        if (gClient.isConnected())
            gClient.disconnect();
    }

    private void startLocUpds() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
            }, 1);
        } else {
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
        stopLocUpds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gClient.isConnected() && locUpd)
            startLocUpds();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                startLocUpds();
                Toast.makeText(this, "Getting your location", Toast.LENGTH_LONG).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Enable location in Settings", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        startLocUpds();
    }
}
