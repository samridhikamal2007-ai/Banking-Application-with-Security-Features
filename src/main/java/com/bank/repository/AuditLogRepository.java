package com.bank.repository;

import com.bank.config.DatabaseConfig;
import com.bank.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {

    public boolean save(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, action, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (log.getUserId() != null) {
                ps.setInt(1, log.getUserId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDescription());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AuditLog> findAllWithUsernames() {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.timestamp DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuditLog log = new AuditLog(
                    rs.getInt("id"),
                    rs.getObject("user_id") != null ? rs.getInt("user_id") : null,
                    rs.getString("username") != null ? rs.getString("username") : "SYSTEM / GUEST",
                    rs.getString("action"),
                    rs.getString("description"),
                    rs.getTimestamp("timestamp")
                );
                list.add(log);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
