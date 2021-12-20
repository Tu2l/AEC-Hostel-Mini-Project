package com.aechostel8.admin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.aechostel8.admin.models.AttendanceConstraints;
import com.aechostel8.admin.util.Constants;
import com.aechostel8.admin.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ProgressBar presentStudentProgress;
    private SwitchCompat attendanceSwitch;
    private TextView statusText, presentPercentageText, absentPercentageText, totalPercentageText;
    private ProgressDialog progressDialog;

    private int total, present;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkLogin();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Retrieving data");

        presentStudentProgress = findViewById(R.id.present_student_progressbar);
        attendanceSwitch = findViewById(R.id.attendance_switch);
        statusText = findViewById(R.id.attendance_status);
        presentPercentageText = findViewById(R.id.presentPercentage);
        absentPercentageText = findViewById(R.id.absentPercentage);
        totalPercentageText = findViewById(R.id.totalText);
        attendanceSwitch.setOnClickListener(v -> {
            progressDialog.show();
            db.collection(Constants.DB.HOSTEL8_PARENT_COLLECTION)
                    .document(Constants.DB.CONSTRAINTS_DOCUMENT)
                    .update("active", attendanceSwitch.isChecked());
        });

        TextView date = findViewById(R.id.date_text);
        date.setText(Utils.getDate());

        checkAdminPermission(auth.getCurrentUser().getEmail());

    }

    private void checkAdminPermission(String email) {
        //verify admin in db
        FirebaseFirestore.getInstance()
                .collection(Constants.DB.ADMINS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful() && task.getResult() != null) {
                        getAttendanceConstraints();
                    } else {
                        if (task.getException() != null)
                            Utils.showAlert(DashboardActivity.this, "Failed: " + task.getException().getMessage());
                        else {
                            Utils.showAlert(DashboardActivity.this, "You don't have admin privileges");
                            auth.signOut();
                            finish();
                        }
                    }
                });
    }

    private void getAttendanceConstraints() {
        progressDialog.show();
        db.collection(Constants.DB.HOSTEL8_PARENT_COLLECTION)
                .document(Constants.DB.CONSTRAINTS_DOCUMENT)
                .addSnapshotListener((value, error) -> {
                    getNumOfApprovedUsers();

                    if (value != null && value.exists()) {
                        AttendanceConstraints constraints = value.toObject(AttendanceConstraints.class);
                        if (constraints != null) {
                            attendanceSwitch.setChecked(constraints.isActive());
                            statusText.setText(constraints.isActive() ? "Attendance is active" : "Attendance is inactive");
                        }

                    } else if (error != null)
                        Utils.showAlert(DashboardActivity.this, "Failed: " + error.getMessage());
                    else
                        Utils.showAlert(DashboardActivity.this, "Something went wrong.");
                });
    }

    private void getNumOfApprovedUsers() {
        db.collection(Constants.DB.CLIENTS_COLLECTION)
                .whereEqualTo("approved", true)
                .get()
                .addOnCompleteListener(task -> {
                    getNumOfPresentUsers();
                    if (task.isSuccessful() && task.getResult() != null) {
                        total = task.getResult().size();
                        initProgress();
                        setReports();
                        Utils.showToastMessage(DashboardActivity.this, "Approved users " + total);
                    } else if (task.getException() != null)
                        Utils.showAlert(DashboardActivity.this, "Failed: " + task.getException().getMessage());
                    else
                        Utils.showAlert(DashboardActivity.this, "Something went wrong.");
                });

    }

    private void getNumOfPresentUsers() {
        db.collection(Constants.DB.ATTENDANCE_COLLECTION + "/" + Utils.getDate())
                .addSnapshotListener((value, error) -> {
                    progressDialog.dismiss();
                    if (value != null) {
                        if (!value.isEmpty()) {
                            present = value.size();
                            setReports();
//                                Utils.showToastMessage(DashboardActivity.this,"present"+value.size());
                        }
                    } else if (error != null)
                        Utils.showAlert(DashboardActivity.this, "Failed: " + error.getMessage());
                    else
                        Utils.showAlert(DashboardActivity.this, "Something went wrong.");
                });
    }


    private void checkLogin() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }


    public void approvalRequests(View view) {
        startActivity(new Intent(this, ApprovalActivity.class));
    }

    public void attendanceReports(View view) {
        startActivity(new Intent(this, ReportsActivity.class));
    }

    private void initProgress() {
        presentStudentProgress.setMax(total);
    }

    private void setReports() {
        presentStudentProgress.setProgress(present);
        totalPercentageText.setText(String.valueOf(total));
        presentPercentageText.setText(String.valueOf(present));
        absentPercentageText.setText(String.valueOf(total - present));
    }
}