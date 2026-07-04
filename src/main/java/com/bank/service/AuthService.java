package com.bank.service;

import com.bank.model.AuditLog;
import com.bank.model.User;
import com.bank.repository.AuditLogRepository;
import com.bank.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

public class AuthService {
    private final UserRepository userRepo = new UserRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    public enum LoginResult {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        LOCKED_OUT,
        BLOCKED // Admin manual block
    }

    public static class AuthResponse {
        private final LoginResult result;
        private final User user;

        public AuthResponse(LoginResult result, User user) {
            this.result = result;
            this.user = user;
        }

        public LoginResult getResult() { return result; }
        public User getUser() { return user; }
    }

    public boolean register(String username, String password, String email, String phone) {
        if (userRepo.findByUsername(username) != null || userRepo.findByEmail(email) != null) {
            return false;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashed);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("CUSTOMER");
        user.setStatus("ACTIVE");
        user.setFailedAttempts(0);

        boolean saved = userRepo.save(user);
        if (saved) {
            AuditLog log = new AuditLog();
            log.setUserId(user.getId());
            log.setAction("REGISTRATION");
            log.setDescription("User registered: " + username);
            auditRepo.save(log);
        }
        return saved;
    }

    public AuthResponse authenticate(String username, String password) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return new AuthResponse(LoginResult.USER_NOT_FOUND, null);
        }

        // Check if status is manually BLOCKED by admin
        if ("BLOCKED".equals(user.getStatus())) {
            return new AuthResponse(LoginResult.BLOCKED, null);
        }

        // Check if locked out
        if (user.getLockoutTime() != null) {
            long currentTime = System.currentTimeMillis();
            long lockTime = user.getLockoutTime().getTime();
            if (currentTime < lockTime) {
                return new AuthResponse(LoginResult.LOCKED_OUT, null);
            } else {
                // Lock expired, reset failed attempts and unlock
                user.setStatus("ACTIVE");
                user.setFailedAttempts(0);
                user.setLockoutTime(null);
                userRepo.update(user);
            }
        }

        // Verify password
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            // Success
            user.setFailedAttempts(0);
            user.setLockoutTime(null);
            userRepo.update(user);

            AuditLog log = new AuditLog();
            log.setUserId(user.getId());
            log.setAction("LOGIN_SUCCESS");
            log.setDescription("User logged in successfully: " + username);
            auditRepo.save(log);

            return new AuthResponse(LoginResult.SUCCESS, user);
        } else {
            // Failed attempt
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            
            AuditLog log = new AuditLog();
            log.setUserId(user.getId());

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus("LOCKED");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, LOCKOUT_DURATION_MINUTES);
                user.setLockoutTime(new Timestamp(cal.getTimeInMillis()));
                userRepo.update(user);

                log.setAction("ACCOUNT_LOCKOUT");
                log.setDescription("User locked out due to consecutive failed logins: " + username);
                auditRepo.save(log);

                return new AuthResponse(LoginResult.LOCKED_OUT, null);
            } else {
                userRepo.update(user);

                log.setAction("LOGIN_FAILED");
                log.setDescription("Failed login attempt " + attempts + " for: " + username);
                auditRepo.save(log);

                return new AuthResponse(LoginResult.WRONG_PASSWORD, null);
            }
        }
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public void updateUserStatus(int userId, String status, int adminUserId) {
        User user = userRepo.findAll().stream().filter(u -> u.getId() == userId).findFirst().orElse(null);
        if (user != null) {
            user.setStatus(status);
            if ("ACTIVE".equals(status)) {
                user.setFailedAttempts(0);
                user.setLockoutTime(null);
            }
            userRepo.update(user);

            AuditLog log = new AuditLog();
            log.setUserId(adminUserId);
            log.setAction("USER_STATUS_CHANGE");
            log.setDescription("Admin updated user " + user.getUsername() + " status to: " + status);
            auditRepo.save(log);
        }
    }
}
