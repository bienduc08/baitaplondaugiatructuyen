package com.uet.auction.server.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbMigrator {
    public static void migrate() {
        System.out.println("[DbMigrator] Bat dau kiem tra va cap nhat cau truc database...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 1. Kiem tra cot step_price
            boolean hasStepPrice = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "step_price")) {
                if (rs.next()) {
                    hasStepPrice = true;
                }
            }
            
            // 2. Kiem tra cot image_url
            boolean hasImageUrl = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "image_url")) {
                if (rs.next()) {
                    hasImageUrl = true;
                }
            }

            try (Statement stmt = conn.createStatement()) {
                if (!hasStepPrice) {
                    System.out.println("[DbMigrator] Thieu cot 'step_price' trong bang 'products'. Dang tien hanh alter table...");
                    String alterSql = "ALTER TABLE products ADD COLUMN step_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00 AFTER current_price";
                    stmt.executeUpdate(alterSql);
                    System.out.println("[DbMigrator] Da them cot 'step_price' thanh cong!");
                } else {
                    System.out.println("[DbMigrator] Cot 'step_price' da ton tai.");
                }

                if (!hasImageUrl) {
                    System.out.println("[DbMigrator] Thieu cot 'image_url' trong bang 'products'. Dang tien hanh alter table...");
                    String alterSql = "ALTER TABLE products ADD COLUMN image_url VARCHAR(255) DEFAULT NULL AFTER status";
                    stmt.executeUpdate(alterSql);
                    System.out.println("[DbMigrator] Da them cot 'image_url' thanh cong!");
                } else {
                    System.out.println("[DbMigrator] Cot 'image_url' da ton tai.");
                }
            }
            System.out.println("[DbMigrator] Hoan tat kiem tra va di tru database!");
            
        } catch (SQLException e) {
            System.err.println("[DbMigrator] Loi nghiem trong khi tu dong di tru database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        migrate();
    }
}
