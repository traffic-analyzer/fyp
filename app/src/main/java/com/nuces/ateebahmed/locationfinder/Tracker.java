package com.nuces.ateebahmed.locationfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
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


public class Tracker extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult> {

    private TextView etLoc;
    protected GoogleApiClient gClient;
    protected Location loc;
    protected LocationRequest locRequest;
    protected boolean reqLocUpds;
    protected static final int CHECK_SETTINGS = 0x0;
    protected LocationSettingsRequest locSettingsReq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        etLoc = (TextView) findViewById(R.id.etLoc);

        reqLocUpds = false;
        buildGClient();
        createLocRequest();
        buildLocSettingsReq();
        checkLocSettings();
    }

    protected synchronized void buildGClient() {
        gClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        reqLocUpds = true;
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "GClient connected", Toast.LENGTH_SHORT).show();
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
            return;
        }*/
        if (loc == null)
            loc = LocationServices.FusedLocationApi.getLastLocation(gClient);
        if (reqLocUpds)
            startLocUpds();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "GClient disconnected", Toast.LENGTH_SHORT).show();
        gClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("TRACKER", connectionResult.getErrorCode() + "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        Log.e("TRACKER", loc.getLatitude() + ":" + loc.getLongitude());
        etLoc.setText(loc.getLatitude() + ":" + loc.getLongitude());

    }

    protected void createLocRequest() {
        locRequest = new LocationRequest();
        locRequest.setInterval(1000);
        locRequest.setFastestInterval(1000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locRequest.setSmallestDisplacement(0);
    }

    private void startLocUpds() {
        LocationServices.FusedLocationApi.requestLocationUpdates(gClient, locRequest, this)
        .setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                reqLocUpds = true;
            }
        });
    }

    private void stopLocUpds() {
        LocationServices.FusedLocationApi.removeLocationUpdates(gClient, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        reqLocUpds = false;
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gClient.isConnected())
            stopLocUpds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gClient.isConnected() && reqLocUpds)
            startLocUpds();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i("Tracker", "Location enabled");
                startLocUpds();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.e("Tracker", "Location disabled");
                try{
                    status.startResolutionForResult(this, CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.e("Tracker", e.getMessage());
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e("Tracker", "Location disabled. No dialog shown");
                break;
        }
    }

    protected void buildLocSettingsReq() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locRequest);
        locSettingsReq = builder.build();
    }

    protected void checkLocSettings() {
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(gClient, locSettingsReq);
        result.setResultCallback(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("Tracker", "Settings changed");
                        startLocUpds();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("Tracker", "Settings not changed");
                        break;
                }
                break;
        }
    }
}
