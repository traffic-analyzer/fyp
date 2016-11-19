package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by progamer on 19/11/16.
 */

public class UserSession {

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Context context;
    private static final int PRIVATE_MODE = 0;
    private static final String SP_NAME = "com.nuces.ateebahmed.locationfinder.PREF_FILE_KEY",
            IS_LOGIN = "isLoggedIn", USERNAME = "username";

    public UserSession() {

    }

    public UserSession(Context context) {
        this.context = context;
        sp = context.getSharedPreferences(SP_NAME, PRIVATE_MODE);
        editor = sp.edit();
    }

    public void createSession(String user) {
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(USERNAME, user);
        editor.commit();
    }

    public String getSPUsername() {
        return sp.getString(USERNAME, null);
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(IS_LOGIN, false);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

}
