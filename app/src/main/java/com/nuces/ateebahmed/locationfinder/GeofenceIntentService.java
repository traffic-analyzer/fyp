package com.nuces.ateebahmed.locationfinder;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by progamer on 11/11/16.
 */

public class GeofenceIntentService extends IntentService {

    protected static final String TAG = "GeofenceIntentService";

    public GeofenceIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            Log.e(TAG, event.getErrorCode() + "");
            return;
        }

        int geofenceTransition = event.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            List<Geofence> trigger = event.getTriggeringGeofences();
            //TODO: notify, get transition details and log
            Log.i(TAG, getTranstitionDetails(geofenceTransition, trigger));
        } else {
            Log.e(TAG, "Error " + geofenceTransition + " on this transition");
        }
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entered in geofence";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exited from geofence";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "Wandering in geofence";
            default:
                return "Unknown transition";
        }
    }

    private String getTranstitionDetails(int transition, List<Geofence> triggers) {
        String trigger = "";
        for (Geofence t: triggers)
            trigger += t.getRequestId();
        return getTransitionString(transition) + " " + trigger;
    }
}

