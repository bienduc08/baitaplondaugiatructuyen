package com.uet.auction;

import com.uet.auction.controller.AuctionController;
import com.uet.auction.controller.AuctionGUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class AuctionApplication {
    // Danh sách để quản lý 3 cửa sổ nhằm cập nhật giá Real-time
    public static List<AuctionGUI> guis = new ArrayList<>();

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        var context = SpringApplication.run(AuctionApplication.class, args);

        // Sửa lỗi getBean: Thêm .class vào sau AuctionController
        AuctionController controller = context.getBean(AuctionController.class);

        String[] users = {"Minh Đức", "Tuấn Anh", "Hoàng Nam"};

        EventQueue.invokeLater(() -> {
            for (int i = 0; i < users.length; i++) {
                AuctionGUI gui = new AuctionGUI(controller, users[i]);
                gui.setTitle("Đấu giá - " + users[i]);
                gui.setLocation(100 + (i * 350), 200);
                gui.setVisible(true);

                // Thêm vào danh sách quản lý
                guis.add(gui);
            }
        });
    }

    // Hàm để gọi từ Controller khi có người bid thành công
    public static void refreshAllGuis() {
        for (AuctionGUI gui : guis) {
            gui.updateData(); // Lệnh cho các cửa sổ cập nhật lại giá
        }
    }
}