package com.aechostel8.admin.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    @SuppressLint("SimpleDateFormat")
    public static String getDate() {
        return new SimpleDateFormat("MMMM dd, yyyy").format(new Date());
    }

    @SuppressLint("SimpleDateFormat")
    public static String getTimeStamp() {
        return new SimpleDateFormat("hh:mm a").format(new Date());
    }

    public static void showToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static boolean validateEmail(String email) {
        Pattern pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static void showAlert(Context context, String message) {
        try {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle("Alert")
                    .setMessage(message)
                    .setNeutralButton("OKAY", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception ex) {
            return "1.0";
        }

    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (Exception ex) {
            return 1;
        }
    }

    public static void registerAttendance(Context context, String uid) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(uid, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(getDate(), true).apply();
    }

    public static boolean isAttendanceRegistered(Context context, String uid) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(uid, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(getDate(), false);
    }

    public static int parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (Exception ex) {
            return -1;
        }
    }

    public static boolean isValidRoomNumber(int room, boolean isNewBuilding) {
        boolean valid = false;
        if (isNewBuilding) {
            valid = room >= 100 && room <= 111 || room > 200 && room <= 214 || room > 300 && room <= 313;
        } else {
            valid = room >= 101 && room <= 109 || room > 200 && room <= 209 || room > 300 && room <= 310;
        }

        return valid;
    }

    public static float getPercentage(float present, float total) {
        return (present / total) * 100;
    }
}
