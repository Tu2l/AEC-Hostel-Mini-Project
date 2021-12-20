package com.aechostel.util;

public class Constants {
    public static final int FINE_LOCATION_PERMISSION_CODE = 10;


    public static class DB {
        public static final String HOSTEL8_PARENT_COLLECTION = "hostel8";
        public static final String CLIENTS_COLLECTION = HOSTEL8_PARENT_COLLECTION + "/users/clients";
        public static final String ADMINS_COLLECTION = HOSTEL8_PARENT_COLLECTION + "/users/admins";
        public static final String ATTENDANCE_COLLECTION = HOSTEL8_PARENT_COLLECTION + "/attendance";
        public static final String CONSTRAINTS_DOCUMENT =  "constraints";
    }
}
