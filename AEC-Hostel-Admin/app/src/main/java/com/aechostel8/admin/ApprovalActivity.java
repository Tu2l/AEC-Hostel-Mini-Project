package com.aechostel8.admin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aechostel8.admin.adapter.UsersAdapter;
import com.aechostel8.admin.models.Filters;
import com.aechostel8.admin.models.HostelUser;
import com.aechostel8.admin.util.Constants;
import com.aechostel8.admin.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;

public class ApprovalActivity extends AppCompatActivity implements UsersAdapter.ClickListener {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private SwipeRefreshLayout refreshLayout;
    private FloatingActionButton filtersFab, approveFab, disapproveFab;
    private UsersAdapter adapter;
    private Filters filters;
    private TextView itemCountText;

    private View actionBar;
    private TextView actionBarText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkLogin();


        initActionBar();

        itemCountText = findViewById(R.id.item_count);
        TextView date = findViewById(R.id.date_text);
        date.setText(Utils.getDate());

        RecyclerView recyclerView = findViewById(R.id.recycler);
        filtersFab = findViewById(R.id.filters_fab);
        approveFab = findViewById(R.id.approve_all_fab);
        disapproveFab = findViewById(R.id.disapprove_all_fab);
        refreshLayout = findViewById(R.id.pull_refresh);
        refreshLayout.setRefreshing(true);
        refreshLayout.setOnRefreshListener(() -> getUsersList());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && filtersFab.isShown() && (!approveFab.isShown() && !disapproveFab.isShown())) {
                    filtersFab.hide();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && (!approveFab.isShown() && !disapproveFab.isShown())) {
                    filtersFab.show();
                }

                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        getUsersList();
        initFab();
    }


    private void initFab() {
        filtersFab.setOnClickListener(v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.custom_view_filters, null);
            dialog.setView(view);
            dialog.setCancelable(false);
            initFilters(view, dialog.show());
        });

        approveFab.setOnClickListener(v -> {
            toggleAuthorization(true);
        });

        disapproveFab.setOnClickListener(v -> {
            toggleAuthorization(false);
        });
    }

    private void toggleAuthorization(boolean b) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage((b ? "Authorize" : "Revoke authorization of") + " all the selected users")
                .setPositiveButton("YES", (dialog, which) -> {
                    dialog.dismiss();

                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("Please wait");
                    progressDialog.setMessage("Updating records");
                    progressDialog.show();

                    WriteBatch batch = db.batch();
                    for (HashMap.Entry<Integer, String> entry : adapter.getSelected().entrySet()) {
                        DocumentReference documentReference = db.collection(Constants.DB.CLIENTS_COLLECTION)
                                .document(entry.getValue());
                        batch.update(documentReference, "approved", b);
                    }

                    batch.commit()
                            .addOnCompleteListener(task -> {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    Utils.showToastMessage(ApprovalActivity.this, "Successfully updated selected records");
                                    getUsersList();
                                } else if (task.getException() != null)
                                    Utils.showAlert(ApprovalActivity.this, "Failed: " + task.getException().getMessage());
                                else
                                    Utils.showAlert(ApprovalActivity.this, "Something went wrong.");
                            });
                })
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show();

    }

    private void initFilters(View view, AlertDialog dialog) {
        final Spinner courseSpinner, branchSpinner, semesterSpinner, blockSpinner, approvalSpinner;
        final Button close, apply;

        courseSpinner = view.findViewById(R.id.spinner_course);
        initSpinner(R.array.course_array, courseSpinner);

        branchSpinner = view.findViewById(R.id.spinner_branch);
        initSpinner(R.array.branch_array, branchSpinner);

        semesterSpinner = view.findViewById(R.id.spinner_semester);
        initSpinner(R.array.semester_array, semesterSpinner);

        blockSpinner = view.findViewById(R.id.spinner_block);
        initSpinner(R.array.block_name_array, blockSpinner);

        approvalSpinner = view.findViewById(R.id.spinner_approval);
        initSpinner(R.array.approval_array, approvalSpinner);


        if (filters != null) {
            if (filters.getBlock() != null) {
                String[] block = getResources().getStringArray(R.array.block_name_array);
                for (int i = 0; i < block.length; i++) {
                    if (TextUtils.equals(block[i], filters.getBlock())) {
                        blockSpinner.setSelection(i);
                        break;
                    }
                }
            }

            if (filters.getCourse() != null) {
                String[] course = getResources().getStringArray(R.array.course_array);
                for (int i = 0; i < course.length; i++) {
                    if (TextUtils.equals(course[i], filters.getCourse())) {
                        courseSpinner.setSelection(i);
                        break;
                    }
                }
            }

            if (filters.getBranch() != null) {
                String[] branch = getResources().getStringArray(R.array.branch_array);
                for (int i = 0; i < branch.length; i++) {
                    if (TextUtils.equals(branch[i], filters.getBranch())) {
                        branchSpinner.setSelection(i);
                        break;
                    }
                }
            }

            semesterSpinner.setSelection(filters.getSemester());

            if (filters.getApproval() != null) {
                String[] approval = getResources().getStringArray(R.array.approval_array);
                for (int i = 0; i < approval.length; i++) {
                    if (TextUtils.equals(approval[i], filters.getApproval())) {
                        approvalSpinner.setSelection(i);
                        break;
                    }
                }
            }
        } else {
            approvalSpinner.setSelection(2);
        }

        close = dialog.findViewById(R.id.close_btn);
        close.setOnClickListener(v -> {
            dialog.dismiss();
        });

        apply = dialog.findViewById(R.id.apply_btn);
        apply.setOnClickListener(v -> {
            if (filters == null)
                filters = new Filters();

            filters.setCourse(String.valueOf(courseSpinner.getSelectedItem()));
            filters.setBranch(String.valueOf(branchSpinner.getSelectedItem()));
            filters.setSemester(semesterSpinner.getSelectedItemPosition());
            filters.setBlock(String.valueOf(blockSpinner.getSelectedItem()));
            filters.setApproval(String.valueOf(approvalSpinner.getSelectedItem()));

            getUsersList();
            dialog.dismiss();
        });

    }

    private void getUsersList() {
        Query query = db.collection(Constants.DB.CLIENTS_COLLECTION);


        if (filters == null) {
            query = query.whereEqualTo("approved", false);
            initRecycler(query);
            return;
        }


        if (filters.getApproval() != null)
            query = query.whereEqualTo("approved", filters.getApproval().equals("Approved"));

        if (filters.getCourse() != null)
            query = query.whereEqualTo("course", filters.getCourse());

        if (filters.getBranch() != null)
            query = query.whereEqualTo("branch", filters.getBranch());

        if (filters.getBlock() != null)
            query = query.whereEqualTo("hostelBlockName", filters.getBlock());

        if (filters.getSemester() != 0)
            query = query.whereEqualTo("semester", filters.getSemester());

        initRecycler(query);

        Log.d("Filters", filters.toString());
    }


    private void initSpinner(int array, Spinner spinner) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                array, R.layout.custom_view_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_view_spinner_drop_down_layout);
        spinner.setAdapter(adapter);
    }

    private void initRecycler(Query query) {
//        adapter.clear();
//        toggleActionBar(false);
//        itemCountText.setText(adapter.getItemCount() + " CLIENT");

        if (!refreshLayout.isRefreshing())
            refreshLayout.setRefreshing(true);

        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        refreshLayout.setRefreshing(false);
                        if (task.isSuccessful()) {
                            if (task.getResult() != null) {
                                adapter.clear();
                                toggleActionBar(false);
                                for (DocumentSnapshot doc : task.getResult().getDocuments())
                                    adapter.add(doc.toObject(HostelUser.class));

                                adapter.notifyDataSetChanged();
                                itemCountText.setText(adapter.getItemCount() + " CLIENT");
                            }
                        } else if (task.getException() != null)
                            Utils.showAlert(ApprovalActivity.this, "Failed: " + task.getException().getMessage());
                        else
                            Utils.showAlert(ApprovalActivity.this, "Something went wrong.");
                    }
                });

    }

    private void checkLogin() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public void onClick(HostelUser user, int pos, boolean longPress) {
        if (!longPress) {
            user.setApproved(!user.isApproved());
            db.collection(Constants.DB.CLIENTS_COLLECTION)
                    .document(user.getEmail())
                    .update("approved", user.isApproved())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            adapter.update(user, pos);
                        } else if (task.getException() != null)
                            Utils.showAlert(ApprovalActivity.this, "Failed: " + task.getException().getMessage());
                        else
                            Utils.showAlert(ApprovalActivity.this, "Something went wrong.");
                    });
        } else {
            toggleActionBar(adapter.isMultiSelect());
            actionBarText.setText("Selected " + adapter.getSelected().size() + "/" + adapter.getItemCount());
        }

    }

    private void initActionBar() {
        actionBar = findViewById(R.id.action_bar);
        actionBar.setVisibility(View.GONE);
        actionBarText = actionBar.findViewById(R.id.selected_title);
        actionBar.findViewById(R.id.close_btn).setOnClickListener(v -> {
            adapter.clearSelected();
            toggleActionBar(false);
        });

        actionBar.findViewById(R.id.select_all).setOnClickListener(v -> {
            adapter.selectALl();
            actionBarText.setText("Selected " + adapter.getSelected().size() + "/" + adapter.getItemCount());
        });
    }


    private void toggleActionBar(boolean show) {
        if (actionBar.getVisibility() == View.VISIBLE && show)
            return;

        if (show) {
            filtersFab.hide();
            approveFab.show();
            disapproveFab.show();
        } else {
            filtersFab.show();
            approveFab.hide();
            disapproveFab.hide();
        }

        actionBar.animate()
                .alpha(show ? 1f : 0f)
                .setDuration(400);
        actionBar.postDelayed(() -> {
            actionBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }, 400);
    }

}