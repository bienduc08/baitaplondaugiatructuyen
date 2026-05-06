package com.uet.auction.server.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    // Định dạng thời gian: Ngày/Tháng/Năm Giờ:Phút:Giây
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Ghi lại thông tin bình thường (INFO)
    public static void info(String message) {
        log("INFO", message);
    }

    // Ghi lại cảnh báo (WARNING)
    public static void warn(String message) {
        log("WARN", message);
    }

    // Ghi lại lỗi hệ thống (ERROR)
    public static void error(String message) {
        log("ERROR", message);
    }

    // Hàm xử lý chung
    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        // Sau này bạn có thể nâng cấp để ghi vào file .txt thay vì chỉ in ra console
        System.out.printf("[%s] [%s] %s%n", timestamp, level, message);
    }
}