package com.aechostel;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.aechostel.models.Attendance;
import com.aechostel.models.AttendanceConstraints;
import com.aechostel.models.HostelUser;
import com.aechostel.util.Constants;
import com.aechostel.util.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class AttendanceActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 11;
    private HostelUser hostelUser;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ImageView imagePreview;
    private Button submitButton;
    private View imageArea;
    private TextView infoText, nameText;

    private ProgressDialog progressDialog;
    private Bitmap image;

    private final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();


    @Override
    protected void onStop() {
        cancellationTokenSource.cancel();
        super.onStop();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        storage = FirebaseStorage.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (firebaseUser == null) finish();

        ((TextView) findViewById(R.id.date_text)).setText(Utils.getDate());

        nameText = findViewById(R.id.username_text);
        nameText.setText(firebaseUser.getDisplayName());

        infoText = findViewById(R.id.info_text);
        imageArea = findViewById(R.id.image_area);
        imagePreview = findViewById(R.id.image_preview);
        submitButton = findViewById(R.id.submit_attendance_btn);

        initProgressDialog();

        if (isGPSOn())
            validateUser();
        else
            Utils.showAlert(this, "Please turn on GPS and restart the app");
    }

    private boolean isGPSOn() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return false;
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Please wait...");

    }

    private void showProgressDialog(String message) {
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        progressDialog.dismiss();
    }

    private void setInputs() {
        nameText.setText(hostelUser.getName());
//        nameText.setOnClickListener(v -> startActivity(new Intent(AttendanceActivity.this, ProfileActivity.class)));
        infoText.setText(getText(R.string.image_instruction));
        imagePreview.setOnClickListener(v -> dispatchTakePictureIntent());
        imageArea.setOnClickListener(v -> dispatchTakePictureIntent());
        submitButton.setOnClickListener(v -> checkDuplicateAttendance());
        submitButton.setEnabled(true);
    }

    private void unsetInputs() {
//        nameText.setOnClickListener(null);
        imagePreview.setOnClickListener(null);
        imageArea.setOnClickListener(null);
        submitButton.setOnClickListener(null);

        image = null;
        imagePreview.setImageBitmap(image);
        imagePreview.setVisibility(View.GONE);
        submitButton.setEnabled(false);
    }

    //validating user
    private void validateUser() {
        showProgressDialog("Checking account status");

        DocumentReference userRef = db.collection(Constants.DB.CLIENTS_COLLECTION).document(firebaseUser.getEmail());
        userRef.addSnapshotListener((value, error) -> {

            hideProgressDialog();

            if (error != null) {
                Utils.showAlert(this, error.getMessage());
                return;
            }

            if (value != null && value.exists()) {
                hostelUser = value.toObject(HostelUser.class);
                if (hostelUser != null) {
                    nameText.setOnClickListener(v -> startActivity(new Intent(AttendanceActivity.this, ProfileActivity.class)));
                    if (Utils.isAttendanceRegistered(AttendanceActivity.this, hostelUser.getUid())) {
                        Utils.showAlert(this, getString(R.string.duplicate_attendance_info));
                        infoText.setText(R.string.duplicate_attendance_info);
                        unsetInputs();
                        return;
                    }
                    if (hostelUser.isApproved()) {
                        // setInputs();
                        checkConstraints();
                    } else {
                        unsetInputs();
                        Utils.showAlert(this, getString(R.string.unapproved_info));
                        infoText.setText(R.string.unapproved_info);
                    }
                }
            } else {
                Utils.showAlert(this, getString(R.string.user_info_not_found));
                infoText.setText(R.string.user_info_not_found);
            }
        });
    }

    private void checkConstraints() {
        showProgressDialog("Collecting information from the server");
        DocumentReference constraintDocRef = db.collection(Constants.DB.HOSTEL8_PARENT_COLLECTION)
                .document(Constants.DB.CONSTRAINTS_DOCUMENT);
        constraintDocRef.addSnapshotListener((value, error) -> {
            hideProgressDialog();

            if (error != null) {
                Utils.showAlert(this, error.getMessage());
                return;
            }

            if (value != null && value.exists()) {
                AttendanceConstraints constraints = value.toObject(AttendanceConstraints.class);
                if (constraints != null) {
                    if (constraints.getVersionCode() != Utils.getVersionCode(AttendanceActivity.this)) {
                        Utils.showAlert(this, getString(R.string.version_error));
                        return;
                    }

                    if (constraints.isActive()) {
//                        Utils.showAlert(this, "Attendance server is active now!");

                        //location check
                        checkLocation(constraints.getConstrainPosition(), constraints.getMaxDistance());

//                        setInputs();

                    } else {
                        unsetInputs();
                        Utils.showAlert(this, "Attendance server is not active now!");
                        infoText.setText("Attendance server is not active now!");
                    }
                }
            }
        });
    }

    private void checkLocation(GeoPoint geoPoint, Double maxDistance) {
        Location mainLocation = new Location("main");
        mainLocation.setLongitude(geoPoint.getLongitude());
        mainLocation.setLatitude(geoPoint.getLatitude());

        showProgressDialog("Checking your location");

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Utils.showAlert(this, "Please Grant Location permission");
            finish();
        }
        fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnCompleteListener(task -> {
                    hideProgressDialog();

                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        if (location != null) {
                            float distance = location.distanceTo(mainLocation);
//                            Log.d("Location", String.valueOf(distance));

                            if (distance <= maxDistance) {
                                setInputs();
                            } else {
                                unsetInputs();
                                Utils.showAlert(AttendanceActivity.this, "You are " + (distance - maxDistance) +
                                        " meters away from the allowed location. Please go back to your hostel and try again.");
                            }
                        }
                    } else if (task.getException() != null) {
                        Utils.showAlert(AttendanceActivity.this, task.getException().getMessage());
                    } else {
                        Utils.showAlert(AttendanceActivity.this, "Failed: Unable to get location data");
                    }
                });
    }

    private void checkDuplicateAttendance() {
        if (image == null) return;

        showProgressDialog("Checking for duplicate attendance");
        //check for existing attendance
        DocumentReference attendanceRef = db.collection(Constants.DB.ATTENDANCE_COLLECTION + "/" + Utils.getDate()).document(hostelUser.getEmail());
        attendanceRef
                .get()
                .addOnCompleteListener(task -> {

                    hideProgressDialog();

                    if (task.isSuccessful()) {
                        if (task.getResult() != null)
                            if (task.getResult().toObject(Attendance.class) == null) {
                                registerAttendance();
                            } else {
                                Utils.registerAttendance(AttendanceActivity.this, hostelUser.getUid());
                                Utils.showAlert(this, getString(R.string.duplicate_attendance_info));
                                infoText.setText(R.string.duplicate_attendance_info);
                                unsetInputs();
                            }
                    } else {
                        if (task.getException() != null) {
                            Utils.showAlert(this, task.getException().getMessage());
                        }
                    }
                });
    }

    private void registerAttendance() {
        showProgressDialog("Registering your attendance please wait");

        String fileName = firebaseUser.getUid() + ".jpg";
        String path = "images/" + Utils.getDate() + "/" + fileName;
        final StorageReference ref = storage.getReference().child(path);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = ref.putBytes(data);
        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null)
                    throw task.getException();
                else
                    throw new Exception("Something went wrong!");
            }

            // Continue with the task to get the download URL
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                saveToFireStore(downloadUri.toString());
            } else {
                hideProgressDialog();
                if (task.getException() != null)
                    Utils.showAlert(AttendanceActivity.this, "Failed: " + task.getException().getMessage());
            }

        });
    }

    private void saveToFireStore(String imageLink) {
        Attendance attendance = new Attendance();
        attendance.setEmail(hostelUser.getEmail());
        attendance.setName(hostelUser.getName());
        attendance.setUid(hostelUser.getUid());
        attendance.setImageUrl(imageLink);
        attendance.setTimeStamp(Utils.getTimeStamp());

        db.collection(Constants.DB.ATTENDANCE_COLLECTION + "/" + Utils.getDate())
                .document(attendance.getEmail())
                .set(attendance)
                .addOnSuccessListener(v -> {
                    unsetInputs();
                    hideProgressDialog();
                    Utils.showAlert(this, getString(R.string.succesfull_attendance));
                    Utils.registerAttendance(AttendanceActivity.this, hostelUser.getUid());
                    infoText.setText(R.string.duplicate_attendance_info);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Utils.showAlert(this, "Failed: " + e.getMessage());
                });

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
            Utils.showAlert(this, e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            image = (Bitmap) extras.get("data");
            imagePreview.setImageBitmap(image);
            imagePreview.setVisibility(View.VISIBLE);
        }
    }


}