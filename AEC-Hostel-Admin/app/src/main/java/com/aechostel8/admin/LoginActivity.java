package com.aechostel8.admin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.aechostel8.admin.util.Constants;
import com.aechostel8.admin.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setMessage("Verifying");
        progressDialog.setCancelable(false);

        checkLogin();
    }

    private void checkLogin() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
    }


    public void resetPassword(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        View alertView = LayoutInflater.from(this).inflate(R.layout.custom_view_reset_password, null);
        alertDialog.setView(alertView);
        alertDialog.show();

        alertView.findViewById(R.id.resetPassword).setOnClickListener(v -> {
            String email = ((EditText) alertView.findViewById(R.id.reset_email)).getText().toString();
            if (!Utils.validateEmail(email)) {
                Utils.showAlert(this, getString(R.string.enter_valid_mail));
                return;
            }
            progressDialog.setMessage("Processing your request");
            progressDialog.show();

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Utils.showAlert(LoginActivity.this, getString(R.string.password_reset_success));
                            alertDialog.dismiss();

                        } else if (task.getException() != null) {
                            Utils.showAlert(LoginActivity.this, "Failed: " + task.getException().getMessage());

                        } else {
                            Utils.showAlert(LoginActivity.this, "Something went wrong when trying to reset your password");
                        }
                    });


//            showToastMessage(email);
//            alertDialog.dismiss();
        });
    }

    public void login(View view) {
        String email = ((EditText) findViewById(R.id.ed_email)).getText().toString();
        String password = ((EditText) findViewById(R.id.ed_password)).getText().toString();

        if (!Utils.validateEmail(email)) {
            Utils.showAlert(this, getString(R.string.enter_valid_mail));
            return;
        }

        if (password.length() < 6) {
            Utils.showAlert(this, getString(R.string.password_info));
            return;
        }

        progressDialog.show();

        verifyEmailPassword(email, password);


    }

    private void verifyEmailPassword(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        checkAdminPermission(email);
                    } else if (task.getException() != null)
                        Utils.showAlert(LoginActivity.this, "Failed: " + task.getException().getMessage());
                    else
                        Utils.showAlert(LoginActivity.this, "Something went wrong when trying to login.");
                });
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
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                    } else {
                        auth.signOut();
                        if (task.getException() != null)
                            Utils.showAlert(LoginActivity.this, "Failed: " + task.getException().getMessage());
                        else
                            Utils.showAlert(LoginActivity.this, "You don't have admin privileges");
                    }
                });
    }

}