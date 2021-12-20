package com.aechostel;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseFirestore.getInstance().clearPersistence();
    }
}
