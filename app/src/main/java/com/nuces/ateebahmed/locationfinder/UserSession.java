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
            IS_LOGIN = "isLoggedIn", USERNAME = "username", DB_KEY = "";

    public UserSession() {

    }

    public UserSession(Context context) {
        this.context = context;
        sp = context.getSharedPreferences(SP_NAME, PRIVATE_MODE);
        editor = sp.edit();
    }

    public void createSession(String user, String key) {
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(USERNAME, user);
        editor.putString(DB_KEY, key);
        editor.commit();
    }

    public String getSPUsername() {
        return sp.getString(USERNAME, null);
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(IS_LOGIN, false);
    }

    public String getDbKey() {
        return  sp.getString(DB_KEY, null);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

}
