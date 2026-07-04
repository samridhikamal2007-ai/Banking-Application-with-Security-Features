package com.bank;

import com.bank.config.DatabaseConfig;
import java.sql.Connection;

public class DbInitRunner {
    public static void main(String[] args) {
        try {
            System.out.println("Executing manual database initialization and seeding...");
            Connection conn = DatabaseConfig.getConnection();
            if (conn != null) {
                System.out.println("Connection successful! Database and tables seeded.");
                conn.close();
            } else {
                System.err.println("Failed to obtain connection.");
            }
        } catch (Exception e) {
            System.err.println("Error running database seeder: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
