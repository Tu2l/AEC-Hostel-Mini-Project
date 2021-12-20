package com.aechostel8.admin;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseFirestore.getInstance().clearPersistence();
    }
}
