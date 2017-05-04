package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Created by progamer on 28/1/17.
 */

public class LocationComponentsSingleton {

    private static LocationComponentsSingleton instance;
    private static final String TAG = "LocComponentsSingleton";
    private Context context;
    private static final long FASTEST_INTERVAL_UPDATE = 5000, INTERVAL_UPDATE = 30000;
    private GoogleApiClient googleApiClient;
    private Location location;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private static final LatLng SOUTH_WEST = new LatLng(24.747001, 66.640322),
                                NORTH_EAST = new LatLng(25.249834, 67.456057);
    protected static final LatLngBounds BOUNDS = LatLngBounds.builder().include(SOUTH_WEST)
                                                                    .include(NORTH_EAST).build();
    /*new LatLngBounds(new LatLng(24.747001, 66.640322),
            new LatLng(25.249834, 67.456057));*/

    private LocationComponentsSingleton(Context c) {
        context = c;
        location = null;
        buildGoogleClient();
        buildLocationRequest();
        buildLocationSettings();
    }

    public synchronized static LocationComponentsSingleton getInstance(Context c) {
        if (instance == null)
            instance = new LocationComponentsSingleton(c.getApplicationContext());
        return instance;
    }

    protected synchronized void buildGoogleClient() {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).build();
    }

    protected synchronized void buildLocationRequest() {
        locationRequest = new LocationRequest().setInterval(INTERVAL_UPDATE)
                .setFastestInterval(FASTEST_INTERVAL_UPDATE);
    }

    protected synchronized void buildLocationSettings() {
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).setAlwaysShow(true).build();
    }

    public void setLocationPriority(int priority) {
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location loc) { location = loc; }

    public boolean isLocationAvailable() { return location != null; }
}
