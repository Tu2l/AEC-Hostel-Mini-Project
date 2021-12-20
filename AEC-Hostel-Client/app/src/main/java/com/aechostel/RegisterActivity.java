package com.aechostel;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.aechostel.models.HostelUser;
import com.aechostel.util.Constants;
import com.aechostel.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    private Spinner courseSpinner;
    private Spinner branchSpinner;
    private Spinner semesterSpinner;
    private Spinner blockSpinner;

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, AttendanceActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        courseSpinner = findViewById(R.id.spinner_course);
        initSpinner(R.array.course_array, courseSpinner);

        branchSpinner = findViewById(R.id.spinner_branch);
        initSpinner(R.array.branch_array, branchSpinner);

        semesterSpinner = findViewById(R.id.spinner_semester);
        initSpinner(R.array.semester_array, semesterSpinner);

        blockSpinner = findViewById(R.id.spinner_block_name);
        initSpinner(R.array.block_name_array, blockSpinner);
    }

    private void initSpinner(int array, Spinner spinner) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                array, R.layout.custom_view_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_view_spinner_drop_down_layout);
        spinner.setAdapter(adapter);
    }

    public void login(View view) {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    public void register(View view) {
        HostelUser user = getUserFromInput();
        if (user == null) return;

        String password = ((EditText) findViewById(R.id.ed_password)).getText().toString();
        if (password.length() < 6) {
            Utils.showAlert(this, getString(R.string.password_info));
            return;
        }

        String confirmPassword = ((EditText) findViewById(R.id.ed_confirm_password)).getText().toString();
        if (!TextUtils.equals(password, confirmPassword)) {
            Utils.showAlert(this, "Password didn't matched. Both confirm password and password must be same!");
            return;
        }


        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Registering user");
        progressDialog.show();

        auth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if (firebaseUser != null) {
                            user.setUid(firebaseUser.getUid());

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(user.getName())
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(task1 -> {
//                                        if (task1.isSuccessful())

                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection(Constants.DB.CLIENTS_COLLECTION)
                                                .document(user.getEmail())
                                                .set(user)
                                                .addOnSuccessListener(aVoid -> {
                                                    progressDialog.dismiss();
                                                    startActivity(new Intent(RegisterActivity.this, AttendanceActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                })
                                                .addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    Utils.showToastMessage(this, e.getMessage());
                                                });
                                    });
                        }

                    } else {
                        if (task.getException() != null)
                            Utils.showAlert(this, "Failed: " + task.getException().getMessage());
                        else
                            Utils.showAlert(this, "Something went wrong while registering");
                    }
                });
    }


    private HostelUser getUserFromInput() {
        HostelUser user = null;

        String email = ((EditText) findViewById(R.id.ed_email)).getText().toString();
        if (!Utils.validateEmail(email)) {
            Utils.showToastMessage(this, "Enter a valid email");
            return user;
        }

        String name = ((EditText) findViewById(R.id.ed_name)).getText().toString();
        if (name.length() < 2) {
            Utils.showToastMessage(this, "Enter a valid name");
            return user;
        }

        String phone = ((EditText) findViewById(R.id.ed_phone)).getText().toString();
        if (phone.length() != 10) {
            Utils.showToastMessage(this, "Enter a valid phone number");
            return user;
        }

        String roll = ((EditText) findViewById(R.id.ed_roll)).getText().toString();
        if (roll.length() == 0) {
            Utils.showToastMessage(this, "Enter a valid roll number");
            return user;
        }

        if (courseSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your course");
            return user;
        }

        if (branchSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your branch");
            return user;
        }

        if (semesterSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your semester");
            return user;
        }

        if (blockSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your block");
            return user;
        }

        String hostelRoomNo = ((EditText) findViewById(R.id.ed_hostel_room_no)).getText().toString();
        if (TextUtils.isEmpty(hostelRoomNo)) {
            Utils.showToastMessage(this, "Please enter your room number");
            return null;
        }

        int room = Utils.parseInt(hostelRoomNo);
        if (!Utils.isValidRoomNumber(room, blockSpinner.getSelectedItemPosition() == 1)) {
            Utils.showAlert(this, "Please enter a valid room number");
            return null;
        }

        user = new HostelUser();
        user.setApproved(false);
        user.setCanEdit(false);
        user.setEmail(email);
        user.setName(name);
        user.setPhone(phone);
        user.setRoll(roll);
        user.setCourse(String.valueOf(courseSpinner.getSelectedItem()));
        user.setBranch(String.valueOf(branchSpinner.getSelectedItem()));
        user.setSemester(semesterSpinner.getSelectedItemPosition());
        user.setHostelBlockName(String.valueOf(blockSpinner.getSelectedItem()));
        user.setHostelRoomNumber(room);
        return user;
    }
}