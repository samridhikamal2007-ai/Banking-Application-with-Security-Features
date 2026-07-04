package com.bank.service;

import com.bank.config.DatabaseConfig;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Random;

public class OtpService {
    private final AuditLogRepository auditRepo = new AuditLogRepository();
    private static final int EXPIRATION_MINUTES = 3;
    private static String lastGeneratedOtp = ""; // Helper to show in UI banner for local testing

    public static String getLastGeneratedOtp() {
        return lastGeneratedOtp;
    }

    public String generateOtp(int userId, String purpose) {
        Random rand = new Random();
        String code = String.format("%06d", rand.nextInt(1000000));
        lastGeneratedOtp = code; // Cache for testing simulator banner

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, EXPIRATION_MINUTES);
        Timestamp expiresAt = new Timestamp(cal.getTimeInMillis());

        String sql = "INSERT INTO otp_verifications (user_id, otp_code, purpose, expires_at, verified) VALUES (?, ?, ?, ?, FALSE)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setString(3, purpose);
            ps.setTimestamp(4, expiresAt);
            ps.executeUpdate();

            // Print to console for simulation verification
            System.out.println("=========================================");
            System.out.println("SIMULATED OTP DISPATCH for User ID: " + userId);
            System.out.println("OTP CODE: " + code);
            System.out.println("PURPOSE: " + purpose);
            System.out.println("EXPIRES IN: " + EXPIRATION_MINUTES + " MINUTES");
            System.out.println("=========================================");

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("OTP_GENERATED");
            log.setDescription("Generated OTP for purpose: " + purpose);
            auditRepo.save(log);

            return code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifyOtp(int userId, String code, String purpose) {
        String sql = "SELECT * FROM otp_verifications WHERE user_id = ? AND purpose = ? AND verified = FALSE ORDER BY expires_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, purpose);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedCode = rs.getString("otp_code");
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    int id = rs.getInt("id");

                    if (System.currentTimeMillis() > expiresAt.getTime()) {
                        System.err.println("OTP code expired.");
                        return false;
                    }

                    if (storedCode.equals(code.trim())) {
                        // Mark verified
                        String updateSql = "UPDATE otp_verifications SET verified = TRUE WHERE id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setInt(1, id);
                            updatePs.executeUpdate();
                        }

                        AuditLog log = new AuditLog();
                        log.setUserId(userId);
                        log.setAction("OTP_VERIFIED");
                        log.setDescription("Successfully verified OTP for purpose: " + purpose);
                        auditRepo.save(log);

                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
