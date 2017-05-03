package com.nuces.ateebahmed.locationfinder;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by progamer on 01/05/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingSrvc";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i(TAG, remoteMessage.getFrom());
        if (remoteMessage.getNotification() != null)
            Log.i(TAG, remoteMessage.getNotification().getBody());
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }
}
