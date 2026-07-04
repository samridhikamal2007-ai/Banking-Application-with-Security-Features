package com.bank.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseConfig {
    private static final String DB_URL = "jdbc:h2:file:./data/banking_db;AUTO_SERVER=TRUE;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    static {
        try {
            Class.forName("org.h2.Driver");
            initializeDatabase();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void initializeDatabase() {
        try (Connection dbConn = getConnection();
                Statement dbStmt = dbConn.createStatement()) {

            // 1. Read schema.sql
            InputStream is = DatabaseConfig.class.getResourceAsStream("/db/schema.sql");
            if (is == null) {
                System.err.println("schema.sql not found in resources!");
                return;
            }

            String schemaSql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                schemaSql = reader.lines()
                        .map(line -> {
                            int index = line.indexOf("--");
                            return index != -1 ? line.substring(0, index) : line;
                        })
                        .collect(Collectors.joining("\n"));
            }

            // Split statements by semicolon
            String[] queries = schemaSql.split(";");
            for (String query : queries) {
                String trimmedQuery = query.trim();
                if (!trimmedQuery.isEmpty()) {
                    dbStmt.executeUpdate(trimmedQuery);
                }
            }
            System.out.println("Database tables initialized successfully.");

            // Seeding Logic: Seed default users if empty
            try (java.sql.ResultSet rs = dbStmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Seeding default bank accounts and user profiles...");

                    // Hash passwords
                    String adminPassHash = org.mindrot.jbcrypt.BCrypt.hashpw("Admin@123",
                            org.mindrot.jbcrypt.BCrypt.gensalt(10));
                    String customerPassHash = org.mindrot.jbcrypt.BCrypt.hashpw("Customer@123",
                            org.mindrot.jbcrypt.BCrypt.gensalt(10));

                    // Insert Admin
                    String insertAdmin = "INSERT INTO users (username, password_hash, email, phone, role, status) VALUES "
                            +
                            "('admin', '" + adminPassHash
                            + "', 'admin@bank.com', '+919999999999', 'ADMIN', 'ACTIVE')";
                    dbStmt.executeUpdate(insertAdmin);

                    // Insert Customer
                    String insertCustomer = "INSERT INTO users (username, password_hash, email, phone, role, status) VALUES "
                            +
                            "('customer', '" + customerPassHash
                            + "', 'customer@bank.com', '+918888888888', 'CUSTOMER', 'ACTIVE')";
                    dbStmt.executeUpdate(insertCustomer, Statement.RETURN_GENERATED_KEYS);

                    int customerId = -1;
                    try (java.sql.ResultSet keys = dbStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            customerId = keys.getInt(1);
                        }
                    }

                    if (customerId != -1) {
                        // Seed checking account
                        String insertChecking = "INSERT INTO accounts (account_number, user_id, account_type, balance, status) VALUES "
                                +
                                "('1001112222', " + customerId + ", 'CHECKING', 10000.00, 'ACTIVE')";
                        dbStmt.executeUpdate(insertChecking);

                        // Seed savings account
                        String insertSavings = "INSERT INTO accounts (account_number, user_id, account_type, balance, status) VALUES "
                                +
                                "('1003334444', " + customerId + ", 'SAVINGS', 5000.00, 'ACTIVE')";
                        dbStmt.executeUpdate(insertSavings);

                        System.out.println(
                                "Default user 'customer' seeded with Checking (1001112222, $10,000) and Savings (1003334444, $5,000) accounts.");
                        System.out.println("Default user 'admin' seeded with ADMIN privileges.");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error initializing database schema or seeding: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
