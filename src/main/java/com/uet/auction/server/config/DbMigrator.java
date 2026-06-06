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

            boolean hasStepPrice = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "step_price")) {
                if (rs.next()) hasStepPrice = true;
            }

            boolean hasImageUrl = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "image_url")) {
                if (rs.next()) hasImageUrl = true;
            }

            boolean hasExtensionCount = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "extension_count")) {
                if (rs.next()) hasExtensionCount = true;
            }

            boolean hasLastExtended = false;
            try (ResultSet rs = metaData.getColumns(null, null, "products", "last_extended_at")) {
                if (rs.next()) hasLastExtended = true;
            }

            try (Statement stmt = conn.createStatement()) {
                if (!hasStepPrice) {
                    stmt.executeUpdate("ALTER TABLE products ADD COLUMN step_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00 AFTER current_price");
                    System.out.println("[DbMigrator] Da them cot 'step_price'.");
                } else {
                    System.out.println("[DbMigrator] Cot 'step_price' da ton tai.");
                }

                if (!hasImageUrl) {
                    stmt.executeUpdate("ALTER TABLE products ADD COLUMN image_url VARCHAR(255) DEFAULT NULL AFTER status");
                    System.out.println("[DbMigrator] Da them cot 'image_url'.");
                } else {
                    System.out.println("[DbMigrator] Cot 'image_url' da ton tai.");
                }

                if (!hasExtensionCount) {
                    stmt.executeUpdate("ALTER TABLE products ADD COLUMN extension_count INT NOT NULL DEFAULT 0");
                    System.out.println("[DbMigrator] Da them cot 'extension_count'.");
                } else {
                    System.out.println("[DbMigrator] Cot 'extension_count' da ton tai.");
                }

                // QUAN TRONG: last_extended_at phai la DATETIME(3) de luu millisecond,
                // tranh timer 1s gia han nhieu lan cho cung 1 bid
                if (!hasLastExtended) {
                    stmt.executeUpdate("ALTER TABLE products ADD COLUMN last_extended_at DATETIME(3) NULL DEFAULT NULL");
                    System.out.println("[DbMigrator] Da them cot 'last_extended_at' (DATETIME(3)).");
                } else {
                    stmt.executeUpdate("ALTER TABLE products MODIFY COLUMN last_extended_at DATETIME(3) NULL DEFAULT NULL");
                    System.out.println("[DbMigrator] Da cap nhat cot 'last_extended_at' sang DATETIME(3).");
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