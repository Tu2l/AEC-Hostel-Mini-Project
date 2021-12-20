package com.aechostel;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aechostel.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();

        checkLogin();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    private void checkLogin() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, AttendanceActivity.class));
            finish();
        }
    }

    public void register(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
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
            ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setTitle("Please wait");
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

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setMessage("Verifying user");
        progressDialog.setCancelable(false);
        progressDialog.show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, AttendanceActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        } else if (task.getException() != null)
                            Utils.showAlert(LoginActivity.this, "Failed: " + task.getException().getMessage());
                        else
                            Utils.showAlert(LoginActivity.this, "Something went wrong when trying to login.");
                    }
                });
    }

}