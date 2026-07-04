package com.bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Account {
    private int id;
    private String accountNumber;
    private int userId;
    private String accountType; // SAVINGS, CHECKING
    private BigDecimal balance;
    private String status; // ACTIVE, SUSPENDED
    private Timestamp createdAt;

    public Account() {}

    public Account(int id, String accountNumber, int userId, String accountType, BigDecimal balance, String status, Timestamp createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
