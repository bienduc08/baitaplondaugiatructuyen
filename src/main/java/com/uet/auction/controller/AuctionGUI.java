package com.uet.auction.controller;

import com.uet.auction.model.User;

import javax.swing.*;
import java.awt.*;

public class AuctionGUI extends JFrame {
    private AuctionController auctionController;
    private String user;

    public AuctionGUI(AuctionController controller, String user) {
        this.auctionController = controller;
        this.user = user;

        // Cấu hình cửa sổ
        setTitle("UET Auction System - Minh Đức");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Hiển thị giữa màn hình
        setLayout(new FlowLayout());

        // Tạo nút bấm
        JButton bidButton = new JButton("Đặt giá (Bid)");

        // Sự kiện khi nhấn nút
        bidButton.addActionListener(e -> {
            // Tạo request giả lập (Sau này lấy từ ô nhập liệu)
            User MinhDuc = new User(1,"Minhduc20", User.Role.USER,"12345",200000);
            BidRequest request = new BidRequest(MinhDuc, 101, 5000.0);

            // Gọi sang Controller
            boolean result = auctionController.onBidButtonClick(request);

            // Hiện thông báo kết quả lên màn hình
            JOptionPane.showMessageDialog(this, result);
        });

        add(bidButton);
    }

    public void updateData() {
    }
}