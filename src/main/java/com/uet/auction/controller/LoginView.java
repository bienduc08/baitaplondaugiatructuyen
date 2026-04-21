package com.uet.auction.controller;

import com.uet.auction.model.User;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import static com.uet.auction.model.User.Role.*;

public class LoginView extends VBox {
    public LoginView() {
        this.setSpacing(10);

        Label lblUser = new Label("Tên người dùng:");
        TextField txtUser = new TextField();

        Label lblRole = new Label("Chọn vai trò:");
        // Lấy các giá trị từ enum Role bạn đã tạo
        ComboBox<User.Role> cbRole = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        cbRole.setValue(USER); // Mặc định là Người mua

        Button btnLogin = new Button("Vào hệ thống");
        btnLogin.setOnAction(e -> {
            String name = txtUser.getText();
            User.Role role = cbRole.getValue();
            System.out.println("Đăng nhập: " + name + " quyền " + role);

            // Ở đây Đức sẽ viết logic để mở cửa sổ tương ứng
            // if (role == User.Role.ADMIN) openAdminStage(); ...
        });

        this.getChildren().addAll(lblUser, txtUser, lblRole, cbRole, btnLogin);
    }
    public void handleLogin(String username, User.Role role) {
        User currentUser = new User(username, role); // Lưu thông tin người đăng nhập

        switch (role) {
            case ADMIN:
                new AdminView(currentUser).show(); // Mở cửa sổ quản trị
                break;
            case SELLER:
                new SellerView(currentUser).show(); // Mở cửa sổ đăng hàng
                break;
            case USER:
                new UserView(currentUser).show(); // Mở cửa sổ đấu giá
                break;
        }
    }

}