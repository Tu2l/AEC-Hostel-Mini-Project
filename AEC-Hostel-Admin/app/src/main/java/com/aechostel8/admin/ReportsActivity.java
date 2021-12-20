package com.aechostel8.admin;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aechostel8.admin.adapter.AttendanceAdapter;
import com.aechostel8.admin.models.Attendance;
import com.aechostel8.admin.util.Constants;
import com.aechostel8.admin.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;

public class ReportsActivity extends AppCompatActivity implements AttendanceAdapter.ClickListener {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private SwipeRefreshLayout refreshLayout;
    private AttendanceAdapter adapter;
    private TextView itemCountText;
    private Button dateButton;

    private String date;
    private int selectYear, selectedMonth, selectedDay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkLogin();

        date = Utils.getDate();
        final Calendar c = Calendar.getInstance();
        selectYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        selectedDay = c.get(Calendar.DAY_OF_MONTH);

        itemCountText = findViewById(R.id.item_count);
        dateButton = findViewById(R.id.date_text);
        dateButton.setText(date);
        dateButton.setOnClickListener(v -> {
            new DatePickerFragment((year, month, day) -> {
                selectYear = year;
                selectedMonth = month;
                selectedDay = day;
                date = new DateFormatSymbols().getMonths()[month] + " " + day + ", " + year;
                dateButton.setText(date);
                getAttendanceList();
            }, selectYear, selectedMonth, selectedDay).show(getSupportFragmentManager(), "date");
        });

        RecyclerView recyclerView = findViewById(R.id.recycler);

        refreshLayout = findViewById(R.id.pull_refresh);
        refreshLayout.setRefreshing(true);
        refreshLayout.setOnRefreshListener(() -> getAttendanceList());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AttendanceAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        getAttendanceList();

        Utils.showToastMessage(this, "Click on the date to select a custom date");
    }


    private void getAttendanceList() {
        Query query = db.collection(Constants.DB.ATTENDANCE_COLLECTION + "/" + date);
        initRecycler(query);
    }

    private void initRecycler(Query query) {
        if (!refreshLayout.isRefreshing())
            refreshLayout.setRefreshing(true);

        query.get()
                .addOnCompleteListener(task -> {
                    refreshLayout.setRefreshing(false);
                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            adapter.clear();
                            for (DocumentSnapshot doc : task.getResult().getDocuments())
                                adapter.add(doc.toObject(Attendance.class));

                            adapter.notifyDataSetChanged();
                            itemCountText.setText(adapter.getItemCount() + " Report");
                        }
                    } else if (task.getException() != null)
                        Utils.showAlert(ReportsActivity.this, "Failed: " + task.getException().getMessage());
                    else
                        Utils.showAlert(ReportsActivity.this, "Something went wrong.");
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
    public void onClick(Attendance attendance) {

    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private final DateListener listener;
        private final int selectedYear, selectedMonth, selectedDay;

        public DatePickerFragment(DateListener listener, int year, int month, int day) {
            this.listener = listener;
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new DatePickerDialog(getActivity(), this, selectedYear, selectedMonth, selectedDay);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            listener.onDateChanged(year, month, day);
        }
    }

    interface DateListener {
        void onDateChanged(int year, int month, int day);
    }

}