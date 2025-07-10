package com.example.workdiary;

public class Faculty {
    private String name;
    private String status;
    private String userId;

    public Faculty(String name, String status, String userId) {
        this.name = name;
        this.status = status;
        this.userId = userId;
    }

    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public void setStatus(String status) { this.status = status; }
}
