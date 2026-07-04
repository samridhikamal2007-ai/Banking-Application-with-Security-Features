package com.bank.service;

import com.bank.config.DatabaseConfig;
import com.bank.model.Account;
import com.bank.model.AuditLog;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditLogRepository;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TransactionService {
    private final AccountRepository accountRepo = new AccountRepository();
    private final TransactionRepository transactionRepo = new TransactionRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public boolean transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description, int userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (fromAccountNumber.equals(toAccountNumber)) return false;

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Enable manual transaction boundary (ACID)

            // 1. Retrieve accounts inside the transaction connection (could do standard queries first)
            Account fromAccount = accountRepo.findByAccountNumber(fromAccountNumber);
            Account toAccount = accountRepo.findByAccountNumber(toAccountNumber);

            if (fromAccount == null || toAccount == null) {
                throw new SQLException("One or both accounts do not exist.");
            }

            if (!"ACTIVE".equals(fromAccount.getStatus()) || !"ACTIVE".equals(toAccount.getStatus())) {
                throw new SQLException("One or both accounts are suspended/inactive.");
            }

            if (fromAccount.getBalance().compareTo(amount) < 0) {
                throw new SQLException("Insufficient funds.");
            }

            // 2. Perform updates
            BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
            BigDecimal newToBalance = toAccount.getBalance().add(amount);

            boolean sourceUpdated = accountRepo.updateBalance(conn, fromAccountNumber, newFromBalance);
            boolean destUpdated = accountRepo.updateBalance(conn, toAccountNumber, newToBalance);

            if (!sourceUpdated || !destUpdated) {
                throw new SQLException("Database error updating balances.");
            }

            // 3. Save transaction record
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction();
            tx.setTransactionId(txId);
            tx.setFromAccount(fromAccountNumber);
            tx.setToAccount(toAccountNumber);
            tx.setTransactionType("TRANSFER");
            tx.setAmount(amount);
            tx.setDescription(description == null || description.trim().isEmpty() ? "Fund Transfer" : description);

            boolean txSaved = transactionRepo.save(conn, tx);
            if (!txSaved) {
                throw new SQLException("Database error saving transaction details.");
            }

            // 4. Commit transaction
            conn.commit();

            // Log security audit
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("TRANSFER");
            log.setDescription("Transferred $" + amount + " from " + fromAccountNumber + " to " + toAccountNumber + ". Transaction ID: " + txId);
            auditRepo.save(log);

            return true;

        } catch (Exception e) {
            System.err.println("Transfer failed, rolling back: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Transaction> getHistory(String accountNumber) {
        return transactionRepo.findByAccountNumber(accountNumber);
    }

    public List<Transaction> getHistoryFiltered(String accountNumber, String type, String startDate, String endDate) {
        return transactionRepo.findByAccountNumberFiltered(accountNumber, type, startDate, endDate);
    }
}
