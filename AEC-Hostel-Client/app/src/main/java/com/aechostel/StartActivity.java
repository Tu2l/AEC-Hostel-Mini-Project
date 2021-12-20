package com.aechostel;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aechostel.util.Constants;
import com.aechostel.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        auth = FirebaseAuth.getInstance();


        TextView versionText = findViewById(R.id.version_text);
        versionText.setText("App Version " + Utils.getVersion(this));

        getLocationPermission();
    }

    private void getLocationPermission() {
        if (!Utils.isLocationPermissionGranted(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        Constants.FINE_LOCATION_PERMISSION_CODE);
            }
        } else {
            launchApp();
        }
    }

    private void launchApp() {
        new Handler().postDelayed(this::checkLogin, 1500);
    }

    private void checkLogin() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, AttendanceActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }

        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.FINE_LOCATION_PERMISSION_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchApp();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_title)
                        .setMessage(R.string.location_permission_desc)
                        .setNegativeButton("Exit", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            dialog.dismiss();
                            getLocationPermission();
                        }).show();
            }
        }

    }
}
