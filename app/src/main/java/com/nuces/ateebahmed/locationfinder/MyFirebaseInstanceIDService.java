package com.nuces.ateebahmed.locationfinder;

import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by progamer on 01/05/17.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {

        FirebaseAuth.AuthStateListener userLoggedIn = getCurrentUser();
        FirebaseAuth user = FirebaseAuth.getInstance();
        user.addAuthStateListener(userLoggedIn);
        String token = FirebaseInstanceId.getInstance().getToken();
        String userId = "";
        if (user.getCurrentUser() != null)
            userId = user.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("token")
                .setValue(token);
    }

    private FirebaseAuth.AuthStateListener getCurrentUser() {
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null)
                    stopSelf();
            }
        };
    }
}
