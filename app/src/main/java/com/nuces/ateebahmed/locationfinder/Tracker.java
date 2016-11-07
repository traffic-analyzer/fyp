package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

public class Tracker extends AppCompatActivity {

    private TextView etLoc;
    private LocationManager locManager;
    private LocationListener locListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        etLoc = (TextView) findViewById(R.id.etLoc);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                etLoc.setText(location.getLatitude() + " : " + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(Tracker.this, "Enable Location service", Toast.LENGTH_SHORT).show();
                Intent access = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(access);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ;
            locManager.removeUpdates(locListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                requestLocation();
        }
    }

    private void requestLocation() {
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Enable Location service", Toast.LENGTH_SHORT).show();
            Intent access = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(access);        } else {
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                viaProvider(LocationManager.NETWORK_PROVIDER);
            else if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                viaProvider(LocationManager.GPS_PROVIDER);
        }
    }

    private void viaProvider(String provider) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
                }, 1);
            }
            return;
        }
        locManager.requestLocationUpdates(provider, 0, 0, locListener);
    }
}
