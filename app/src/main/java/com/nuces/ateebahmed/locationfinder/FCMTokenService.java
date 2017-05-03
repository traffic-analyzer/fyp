package com.nuces.ateebahmed.locationfinder;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FCMTokenService extends FirebaseInstanceIdService {

    static final String TOKEN = "token",
            SP_NAME = "com.nuces.ateebahmed.locationfinder.PREF_FILE_KEY";

    @Override
    public void onTokenRefresh() {
        SharedPreferences sharedPreferences = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String token = FirebaseInstanceId.getInstance().getToken();
        editor.putString(TOKEN, token);
        editor.apply();
    }
}
