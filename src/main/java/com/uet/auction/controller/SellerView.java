package com.uet.auction.controller;

import com.uet.auction.model.Product;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.*;


public class SellerView {
    private User user;
    private DataStorage dataStorage;
    private AuctionService auctionService; // Thêm biến này

    // Giả sử đây là các controls từ giao diện
    @FXML
    private Button btnPost;
    private TextField txtName;
    private TextField txtPrice;
    private TextField txtId;

    public SellerView(User user, DataStorage dataStorage, AuctionService auctionService) {
        this.user = user;
        this.dataStorage = dataStorage;
        this.auctionService = auctionService;
        this.btnPost = new Button("Đăng sản phẩm");
        this.txtName = new TextField(); // Thêm dòng này
        this.txtPrice = new TextField(); // Thêm dòng này
        this.txtId = new TextField();    // Thêm dòng này

        // Gán sự kiện cho nút bấm
        btnPost.setOnAction(e -> handlePostAction());
    }

    private void handlePostAction() {
        try {
            // 1. Lấy và parse dữ liệu
            int id = Integer.parseInt(txtId.getText());
            String name = txtName.getText();
            if (txtName.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Tên sản phẩm không được để trống!").showAndWait();
                return;
            }
            double price = Double.parseDouble(txtPrice.getText());

            // 2. Tạo đối tượng Product (để Admin duyệt)
            Product newP = new Product(id, name, price, user.getUsername());

            // 3. Đưa vào danh sách chờ trong DataStorage
            dataStorage.addProduct(newP);

            // 4. (Tùy chọn) Gửi qua Service nếu bạn có xử lý đặc biệt
            auctionService.sendToAdminForApproval(newP);

            new Alert(Alert.AlertType.INFORMATION, "Đã gửi sản phẩm chờ Admin chốt giờ!").show();

        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Vui lòng nhập đúng định dạng số cho ID và Giá!").show();
        }
    }
}