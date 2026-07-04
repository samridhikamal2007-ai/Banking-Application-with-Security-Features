package com.bank.repository;

import com.bank.config.DatabaseConfig;
import com.bank.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public boolean save(Connection conn, Transaction tx) throws Exception {
        String sql = "INSERT INTO transactions (transaction_id, from_account, to_account, transaction_type, amount, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getFromAccount());
            ps.setString(3, tx.getToAccount());
            ps.setString(4, tx.getTransactionType());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setString(6, tx.getDescription());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        tx.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean save(Transaction tx) {
        String sql = "INSERT INTO transactions (transaction_id, from_account, to_account, transaction_type, amount, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getFromAccount());
            ps.setString(3, tx.getToAccount());
            ps.setString(4, tx.getTransactionType());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setString(6, tx.getDescription());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        tx.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Transaction> findByAccountNumber(String accountNumber) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE from_account = ? OR to_account = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Transaction> findByAccountNumberFiltered(String accountNumber, String type, String startDate, String endDate) {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE (from_account = ? OR to_account = ?)");
        
        List<Object> params = new ArrayList<>();
        params.add(accountNumber);
        params.add(accountNumber);

        if (type != null && !type.equalsIgnoreCase("ALL")) {
            sql.append(" AND transaction_type = ?");
            params.add(type.toUpperCase());
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND timestamp >= ?");
            params.add(Timestamp.valueOf(startDate + " 00:00:00"));
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND timestamp <= ?");
            params.add(Timestamp.valueOf(endDate + " 23:59:59"));
        }

        sql.append(" ORDER BY timestamp DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws Exception {
        return new Transaction(
            rs.getInt("id"),
            rs.getString("transaction_id"),
            rs.getString("from_account"),
            rs.getString("to_account"),
            rs.getString("transaction_type"),
            rs.getBigDecimal("amount"),
            rs.getString("description"),
            rs.getTimestamp("timestamp")
        );
    }
}
