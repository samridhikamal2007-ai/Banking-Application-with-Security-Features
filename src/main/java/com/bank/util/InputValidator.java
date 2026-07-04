package com.bank.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    
    // Password needs: 8+ chars, uppercase, lowercase, digit, special character
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?|]");

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        return PASSWORD_UPPERCASE.matcher(password).find() &&
               PASSWORD_LOWERCASE.matcher(password).find() &&
               PASSWORD_DIGIT.matcher(password).find() &&
               PASSWORD_SPECIAL.matcher(password).find();
    }

    public static BigDecimal validateAndParseAmount(String amountStr) {
        if (amountStr == null) return null;
        try {
            BigDecimal amt = new BigDecimal(amountStr.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return amt;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        String trimmed = accountNumber.trim();
        return trimmed.length() == 10 && trimmed.matches("\\d+");
    }

    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
}
