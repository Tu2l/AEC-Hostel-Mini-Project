package com.aechostel;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aechostel.models.HostelUser;
import com.aechostel.util.Constants;
import com.aechostel.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {
    private HostelUser hostelUser;

    private Spinner courseSpinner;
    private Spinner branchSpinner;
    private Spinner semesterSpinner;
    private Spinner blockSpinner;
    private EditText rollEditText;
    private EditText hostelRoomNoEditText;
    private Button updateBtn;

    boolean editing = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        TextView date = findViewById(R.id.date_text);
        date.setText(Utils.getDate());
        Button editProfileBtn = findViewById(R.id.edit_profile);
        editProfileBtn.setOnClickListener(v -> {
            if (editing) {
                editing = false;
                setInput();
                toggleInputEnabled(editing);
                editProfileBtn.setText(getText(R.string.edit_profile));
                return;
            }

            if (hostelUser != null && hostelUser.isCanEdit()) {
                editing = true;
                toggleInputEnabled(true);
                editProfileBtn.setText("Cancel Editing");
            } else {
                Utils.showAlert(ProfileActivity.this, getString(R.string.update_permission_denied));
            }
        });
        Button logOutBtn = findViewById(R.id.log_out);
        logOutBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle(getText(R.string.log_out))
                    .setMessage("Are you sure you want to Log Out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();

                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        rollEditText = findViewById(R.id.ed_roll);
        hostelRoomNoEditText = findViewById(R.id.ed_hostel_room_no);
        updateBtn = findViewById(R.id.update_profile_btn);

        courseSpinner = findViewById(R.id.spinner_course);
        initSpinner(R.array.course_array, courseSpinner);

        branchSpinner = findViewById(R.id.spinner_branch);
        initSpinner(R.array.branch_array, branchSpinner);

        semesterSpinner = findViewById(R.id.spinner_semester);
        initSpinner(R.array.semester_array, semesterSpinner);

        blockSpinner = findViewById(R.id.spinner_block_name);
        initSpinner(R.array.block_name_array, blockSpinner);

        toggleInputEnabled(false);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Please wait...");
        progressDialog.setMessage("Checking account status");
        progressDialog.show();

        DocumentReference userRef = db.collection(Constants.DB.CLIENTS_COLLECTION).document(firebaseUser.getEmail());
        userRef.addSnapshotListener((value, error) -> {
            progressDialog.dismiss();

            if (error != null) {
                Utils.showAlert(this, "Failed: " + error.getMessage());
                return;
            }

            if (value != null && value.exists()) {
                hostelUser = value.toObject(HostelUser.class);
                if (hostelUser != null) {
                    setInput();
                    if (hostelUser.isCanEdit()) {
                        Utils.showAlert(ProfileActivity.this, getString(R.string.update_permission_accepted));

                        updateBtn.setOnClickListener(v -> {
                            if (!getUserFromInput()) return;
                            userRef.set(hostelUser).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Utils.showAlert(ProfileActivity.this, "Profile Updated Successfully");
                                } else if (task.getException() != null) {
                                    Utils.showAlert(this, "Failed: " + task.getException().getMessage());
                                } else {
                                    Utils.showAlert(this, "Something went wrong while updating your profile");
                                }
                            });

                        });

                    } else {
                        Utils.showAlert(ProfileActivity.this, getString(R.string.update_permission_denied));
                        toggleInputEnabled(false);
                        updateBtn.setOnClickListener(null);
                    }
                }
            } else {
                Utils.showAlert(this, getString(R.string.user_info_not_found));
                finish();
            }
        });
    }

    private void setInput() {
        if (hostelUser == null) return;

        rollEditText.setText(hostelUser.getRoll());
        hostelRoomNoEditText.setText(String.valueOf(hostelUser.getHostelRoomNumber()));

        String[] block = getResources().getStringArray(R.array.block_name_array);
        for (int i = 0; i < block.length; i++) {
            if (TextUtils.equals(block[i], hostelUser.getHostelBlockName())) {
                blockSpinner.setSelection(i);
                break;
            }
        }

        String[] course = getResources().getStringArray(R.array.course_array);
        for (int i = 0; i < course.length; i++) {
            if (TextUtils.equals(course[i], hostelUser.getCourse())) {
                courseSpinner.setSelection(i);
                break;
            }
        }

        String[] branch = getResources().getStringArray(R.array.branch_array);
        for (int i = 0; i < branch.length; i++) {
            if (TextUtils.equals(branch[i], hostelUser.getBranch())) {
                branchSpinner.setSelection(i);
                break;
            }
        }

        semesterSpinner.setSelection(hostelUser.getSemester());
    }

    private void toggleInputEnabled(boolean b) {
        rollEditText.setEnabled(b);
        hostelRoomNoEditText.setEnabled(b);
        blockSpinner.setEnabled(b);
        courseSpinner.setEnabled(b);
        branchSpinner.setEnabled(b);
        semesterSpinner.setEnabled(b);

        updateBtn.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void initSpinner(int array, Spinner spinner) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                array, R.layout.custom_view_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_view_spinner_drop_down_layout);
        spinner.setAdapter(adapter);
    }

    private boolean getUserFromInput() {
        if (hostelUser == null) {
            Utils.showAlert(this, "No user data found!");
            return false;
        }

        String roll = rollEditText.getText().toString();
        if (roll.length() == 0) {
            Utils.showToastMessage(this, "Enter a valid roll number");
            return false;
        }

        if (courseSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your course");
            return false;
        }

        if (branchSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your branch");
            return false;
        }

        if (semesterSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your semester");
            return false;
        }


        if (blockSpinner.getSelectedItemPosition() == 0) {
            Utils.showToastMessage(this, "Please select your block");
            return false;
        }

        String hostelRoomNo = ((EditText) findViewById(R.id.ed_hostel_room_no)).getText().toString();
        if (TextUtils.isEmpty(hostelRoomNo)) {
            Utils.showToastMessage(this, "Please enter your room number");
            return false;
        }

        int room = Utils.parseInt(hostelRoomNo);
        if (!Utils.isValidRoomNumber(room, blockSpinner.getSelectedItemPosition() == 1)) {
            Utils.showAlert(this, "Please enter a valid room number");
            return false;
        }

        hostelUser.setRoll(roll);
        hostelUser.setCourse(String.valueOf(courseSpinner.getSelectedItem()));
        hostelUser.setBranch(String.valueOf(branchSpinner.getSelectedItem()));
        hostelUser.setSemester(semesterSpinner.getSelectedItemPosition());
        hostelUser.setHostelBlockName(String.valueOf(blockSpinner.getSelectedItem()));
        hostelUser.setHostelRoomNumber(room);
        return true;
    }

}