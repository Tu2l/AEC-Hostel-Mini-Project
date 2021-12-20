package com.aechostel8.admin.models;

import androidx.annotation.NonNull;

public class Filters {
    String course;
    String branch;
    int semester;
    String block;
    String approval;

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        if (course.contains("All")) {
            this.course = null;
            return;
        }

        this.course = course;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        if (branch.contains("All")) {
            this.branch = null;
            return;
        }
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        if (block.contains("All")) {
            this.block = null;
            return;
        }
        this.block = block;
    }

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
        if (approval.contains("All")) {
            this.approval = null;
            return;
        }
        this.approval = approval;
    }

    @NonNull
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getApproval()).append(" ")
                .append(getBlock()).append(" ")
                .append(getBranch()).append(" ")
                .append(getCourse()).append(" ")
                .append(getSemester()).toString();

    }
}
