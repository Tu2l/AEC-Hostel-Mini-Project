package com.aechostel8.admin.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aechostel8.admin.R;
import com.aechostel8.admin.models.Attendance;
import com.bumptech.glide.Glide;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final ClickListener clickListener;
    private final List<Attendance> attendances;

    public AttendanceAdapter(ClickListener clickListener, List<Attendance> attendances) {
        this.clickListener = clickListener;
        this.attendances = attendances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_view_recycler_view_item_attendance, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance attendance = attendances.get(position);
        holder.bind(attendance);
        holder.itemView.setOnClickListener(v -> {

        });

    }

    @Override
    public int getItemCount() {
        return attendances.size();
    }

    public void clear() {
        attendances.clear();
        notifyDataSetChanged();
    }

    public void add(Attendance attendance) {
        attendances.remove(attendance);
        attendances.add(attendance);
    }

    public void update(Attendance attendance, int index) {
        if (index < attendances.size()) {
            attendances.set(index, attendance);
            notifyItemChanged(index);
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, time;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.nameText);
            time = itemView.findViewById(R.id.timestamp);
            imageView = itemView.findViewById(R.id.image);
        }

        public void bind(Attendance attendance) {
//            Log.d("BIND",attendance.getName());
            name.setText(attendance.getName());
            time.setText(attendance.getTimeStamp());
            Glide.with(itemView.getContext())
                    .load(attendance.getImageUrl())
                    .placeholder(itemView.getContext().getDrawable(R.drawable.ic_baseline_person))
                    .into(imageView);
        }

    }

    public interface ClickListener {
        void onClick(Attendance attendance);
    }
}
