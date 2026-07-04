package com.bank.model;

import java.sql.Timestamp;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String email;
    private String phone;
    private String role; // CUSTOMER, ADMIN
    private String status; // ACTIVE, LOCKED
    private int failedAttempts;
    private Timestamp lockoutTime;
    private Timestamp createdAt;

    public User() {}

    public User(int id, String username, String passwordHash, String email, String phone, String role, String status, int failedAttempts, Timestamp lockoutTime, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.lockoutTime = lockoutTime;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public Timestamp getLockoutTime() { return lockoutTime; }
    public void setLockoutTime(Timestamp lockoutTime) { this.lockoutTime = lockoutTime; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
