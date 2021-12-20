package com.aechostel.models;

import com.google.firebase.firestore.GeoPoint;

public class AttendanceConstraints {
    private boolean active;
    private double maxDistance;
    private GeoPoint constrainPosition;
    private int versionCode;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public GeoPoint getConstrainPosition() {
        return constrainPosition;
    }

    public void setConstrainPosition(GeoPoint constrainPosition) {
        this.constrainPosition = constrainPosition;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
}
