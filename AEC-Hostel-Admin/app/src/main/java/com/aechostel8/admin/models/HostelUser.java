package com.aechostel8.admin.models;

public class HostelUser {
    private String uid;
    private String email;
    private String name;
    private String phone;
    private String roll;
    private String course;
    private String branch;
    private int semester;
    private String hostelBlockName;
    private int hostelRoomNumber;

    private boolean isApproved;
    private boolean canEdit;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getHostelBlockName() {
        return hostelBlockName;
    }

    public void setHostelBlockName(String hostelBlockName) {
        this.hostelBlockName = hostelBlockName;
    }

    public int getHostelRoomNumber() {
        return hostelRoomNumber;
    }

    public void setHostelRoomNumber(int hostelRoomNumber) {
        this.hostelRoomNumber = hostelRoomNumber;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }
}
