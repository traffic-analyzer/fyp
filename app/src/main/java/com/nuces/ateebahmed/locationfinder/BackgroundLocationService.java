package com.nuces.ateebahmed.locationfinder;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BackgroundLocationService extends Service implements LocationListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "BckgrndLocationService";
    private volatile HandlerThread locationHandlerThread;
    private LocationHandler locationHandler;
    private LocalBroadcastManager localBroadcastManager;
    protected static final String ACTION =
            "com.ateebahmed.testingbackgroundlocation.BackgroundLocationService";
    private GoogleApiClient gClient;
    private LocationRequest locationRequest;
    /*private NotificationCompat.Builder notification;
    private NotificationManager notificationManager;
    private final int NID = 10;*/
    private LocationComponentsSingleton instance;
    private BroadcastReceiver locationPermissionReceiver;
    private DatabaseReference dbUserRef, conRef;
    private ValueEventListener connectionListener;
    private Location loc;
    private boolean isConnected;
    private FirebaseAuth userAuth;
    private FirebaseAuth.AuthStateListener userVerified;

    public BackgroundLocationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationHandlerThread = new HandlerThread("BackgroundLocationService.HandlerThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        locationHandlerThread.start();

        locationHandler = new LocationHandler(locationHandlerThread.getLooper());

        locationHandler.post(new Runnable() {
            @Override
            public void run() {
                setInstance();
                isConnected = false;
                connectionListener = checkConnectivity();
                userVerified = getUserVerified();
                localBroadcastManager = LocalBroadcastManager
                        .getInstance(BackgroundLocationService.this);
                userAuth = FirebaseAuth.getInstance();
                setLocationSignalReceiver();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*if (intent != null) {
            if (intent.getStringExtra("alarm") != null) {
                String message = intent.getStringExtra("alarm");
                Log.i(TAG, "alarm said, " + message);
            }
        }*/
        /*Message msg = locationHandler.obtainMessage();
        msg.arg1 = startId;
        locationHandler.sendMessage(msg);*/

        locationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!gClient.isConnected() && !gClient.isConnecting())
                    gClient.connect();
                addConnectionListener();
                addAuthStateListener();
                IntentFilter filter = new IntentFilter(MapsActivity.ACTION);
                localBroadcastManager.registerReceiver(locationPermissionReceiver, filter);
            }
        });
        return START_STICKY;
    }

    @Override
    public void onLocationChanged(final Location location) {
        locationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (loc == null || !loc.equals(location)) {
                    loc = location;
                    sendNewLocation(loc);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "service destroyed");
        localBroadcastManager.unregisterReceiver(locationPermissionReceiver);
        removeAuthStateListener();
        removeConnectionListener();
        stopLocationUpdate();
        destroyGClient();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            locationHandlerThread.quitSafely();
        } else locationHandlerThread.quit();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "connected");
        locationHandler.post(new Runnable() {
            @Override
            public void run() {
                startLocationUpdate();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "suspended");
        locationHandler.post(new Runnable() {
            @Override
            public void run() {
                gClient.connect();
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorCode() + ": " + connectionResult.getErrorMessage());
    }

    private void startLocationUpdate() {
        if (checkLocationPermission()) {
            Log.i(TAG, "starting location updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(gClient, locationRequest, this);
        } else sendLocationRequestSignal();
    }

    private void stopLocationUpdate() {
        if (gClient != null && gClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(gClient, this);
    }

    private void destroyGClient() {
        if (gClient != null)
            if (gClient.isConnecting() || gClient.isConnected()) {
                gClient.unregisterConnectionFailedListener(BackgroundLocationService.this);
                gClient.unregisterConnectionCallbacks(BackgroundLocationService.this);
                gClient.disconnect();
            }
    }

    /*private void createNotification() {
        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(ic_menu_mylocation)
                .setContentTitle("Allow Location Detection")
                .setContentText("Allow location permission to periodically detect your location");

        Intent i = new Intent(getApplicationContext(), MapsActivity.class);
        i.putExtra("service", true);

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 111, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pi);
    }

    private void notifyUser() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NID, notification.build());
    }

    private void buildPendingBroadcast() {
        Intent i = new Intent(this, LocationBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, LocationBroadcastReceiver.REQUEST_CODE,
                i, PendingIntent.FLAG_CANCEL_CURRENT);
    }*/

    private void setLocationSignalReceiver() {
        locationPermissionReceiver = new LocationPermissionBroadcastReceiver();
    }

    private void sendLocationRequestSignal() {
        Log.i(TAG, "sending location request signal");
        Intent i = new Intent(ACTION);
        i.putExtra("request", true);
        localBroadcastManager.sendBroadcast(i);
    }

    private void setInstance() {
        Log.i(TAG, "instance from service");
        if (instance == null) {
            instance = LocationComponentsSingleton.getInstance(this);

            gClient = instance.getGoogleApiClient();
            gClient.registerConnectionCallbacks(this);
            gClient.registerConnectionFailedListener(this);

            locationRequest = instance.getLocationRequest();
        }
    }

    private boolean checkLocationPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void sendNewLocation(Location location) {
        dbUserRef.child("latitude").setValue(location.getLatitude());
        dbUserRef.child("longitude").setValue(location.getLongitude());

        Intent i = new Intent(ACTION);
        i.putExtra("location", location);
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

    private FirebaseAuth.AuthStateListener getUserVerified() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null)
                    stopSelf();
                else {
                    if (dbUserRef == null) {
                        dbUserRef = FirebaseDatabase.getInstance().getReference().child("users")
                                .child(firebaseAuth.getCurrentUser().getUid());
                    }
                }
            }
        };
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

    private final class LocationHandler extends Handler {
        public LocationHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, msg.arg1 + "");
        }
    }
    
    private final class LocationPermissionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("startlocationupdate", false)) {
                instance.setLocationPriority(intent.getIntExtra("priority",
                        LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY));
                gClient.reconnect();
                Log.i(TAG, locationRequest.getPriority() + "");
            } else if(intent.getBooleanExtra("stop", false)) {
                Log.i(TAG, "stopping service");
                stopSelf();
            }
        }
    }
}
