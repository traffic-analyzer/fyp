package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

/**
 * Created by progamer on 28/1/17.
 */

public class LocationComponentsSingleton {

    private static LocationComponentsSingleton instance;
    private static final String TAG = "LocComponentsSingleton";
    private Context context;
    private static final long FASTEST_INTERVAL_UPDATE = 5000, INTERVAL_UPDATE = 30000;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    private LocationComponentsSingleton(Context c) {
        context = c;
        buildGoogleClient();
        buildLocationRequest();
        buildLocationSettings();
    }

    public synchronized static LocationComponentsSingleton getInstance(Context c) {
        if (instance == null) {
            Log.i(TAG, "instance is null");
                instance = new LocationComponentsSingleton(c.getApplicationContext());
        } else Log.i(TAG, "old instance");
        return instance;
    }

    protected synchronized void buildGoogleClient() {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API)
                /*.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.e(TAG, connectionResult.getErrorCode() + ": " +
                        connectionResult.getErrorMessage());
            }
        }).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.i(TAG, "connected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.i(TAG, "connection suspended: " + i);
                        googleApiClient.connect();
                    }
                })*/.build();
    }

    protected synchronized void buildLocationRequest() {
        locationRequest = new LocationRequest().setInterval(INTERVAL_UPDATE)
                .setFastestInterval(FASTEST_INTERVAL_UPDATE)
                /*.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)*/;
    }

    protected synchronized void buildLocationSettings() {
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).setAlwaysShow(true).build();
    }

    public synchronized void setLocationPriority(int priority) {
        if (locationRequest != null)
            locationRequest.setPriority(priority);
        else Log.e(TAG, "initialize location request first");
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    public LocationRequest getLocationRequest() {
        return locationRequest;
    }

    public LocationSettingsRequest getLocationSettingsRequest() {
        return locationSettingsRequest;
    }
}
