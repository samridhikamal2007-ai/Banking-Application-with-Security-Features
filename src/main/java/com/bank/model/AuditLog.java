package com.bank.model;

import java.sql.Timestamp;

public class AuditLog {
    private int id;
    private Integer userId;
    private String username; // Added helper for admin display if joined
    private String action;
    private String description;
    private Timestamp timestamp;

    public AuditLog() {}

    public AuditLog(int id, Integer userId, String username, String action, String description, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
