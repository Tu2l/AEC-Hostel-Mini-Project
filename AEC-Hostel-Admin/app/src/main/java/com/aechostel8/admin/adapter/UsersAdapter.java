package com.aechostel8.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aechostel8.admin.R;
import com.aechostel8.admin.models.HostelUser;

import java.util.HashMap;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private final ClickListener clickListener;
    private final List<HostelUser> users;
    private final HashMap<Integer, String> selected = new HashMap<>();
    private boolean multiSelect;

    public UsersAdapter(ClickListener clickListener, List<HostelUser> users) {
        this.clickListener = clickListener;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_view_recycler_view_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HostelUser user = users.get(position);
        holder.bind(user);
        holder.button.setOnClickListener(v -> {
            clickListener.onClick(user, position, false);
            ((Button) v).setText("Please wait");
            v.setEnabled(false);
        });

        holder.itemView.setOnClickListener(v -> {
            if (multiSelect) {
                if (selected.containsKey(position)) {
                    removeSelected(position);
                    holder.setSelected(false);
                } else {
                    setSelected(position, user.getEmail());
                    holder.setSelected(true);
                }
                clickListener.onClick(user, position, true);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelect) {
                multiSelect = true;
                setSelected(position, user.getEmail());
                holder.setSelected(true);
            } else {
                multiSelect = false;
                clearSelected();
            }

            clickListener.onClick(user, position, true);
            return true;
        });

        holder.setSelected(selected.containsKey(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void clear() {
        users.clear();
        selected.clear();
        multiSelect = false;
        notifyDataSetChanged();
    }

    public void add(HostelUser user) {
        users.remove(user);
        users.add(user);
    }

    public void update(HostelUser user, int index) {
        if (index < users.size()) {
            users.set(index, user);
            notifyItemChanged(index);
        }
    }

    public void selectALl() {
        selected.clear();
        for (int i = 0; i < users.size(); i++)
            selected.put(i, users.get(i).getEmail());

        notifyDataSetChanged();
    }

    public void setSelected(int index, String email) {
        selected.put(index, email);
    }

    public void removeSelected(int index) {
        selected.remove(index);
    }

    public void clearSelected() {
        selected.clear();
        notifyDataSetChanged();
    }

    public HashMap<Integer, String> getSelected() {
        return selected;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, roll, sem, course, branch, block, room;
        Button button;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            roll = itemView.findViewById(R.id.roll);
            sem = itemView.findViewById(R.id.sem);
            course = itemView.findViewById(R.id.course);
            branch = itemView.findViewById(R.id.branch);
            block = itemView.findViewById(R.id.block);
            room = itemView.findViewById(R.id.room);
            button = itemView.findViewById(R.id.button);
        }

        public void bind(HostelUser user) {
            name.setText(user.getName());
            roll.setText("Roll " + user.getRoll());
            sem.setText("Sem " + user.getSemester());
            course.setText(user.getCourse());
            branch.setText(user.getBranch());
            block.setText("Block " + user.getHostelBlockName());
            room.setText("Room " + user.getHostelRoomNumber());


            button.setBackgroundColor(user.isApproved() ? Color.RED : Color.MAGENTA);
            button.setText(user.isApproved() ? "Disapprove" : "Approve");

            button.setEnabled(true);
        }

        public void setSelected(boolean b) {
            itemView.setBackground(
                    itemView.getContext()
                            .getResources().getDrawable(b ? R.drawable.card_bg_selected : R.drawable.card_bg)
            );
        }
    }

    public interface ClickListener {
        void onClick(HostelUser user, int pos, boolean longPress);
    }
}
