package com.example.workdiary;

public class WorkdoneRow {
    public String dayDate;
    public String time;
    public String classField;
    public String course;
    public String portion;
    public String no;
    public String remarks;
    public int week; // This field is used for sorting by week

    public WorkdoneRow() {
        this.dayDate = "";
        this.time = "";
        this.classField = "";
        this.course = "";
        this.portion = "";
        this.no = "";
        this.remarks = "";
        this.week = 0;
    }
}
