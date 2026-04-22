package com.uet.auction.controller;

import com.uet.auction.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.uet.auction.model.User.Role.*;

public class LoginView extends AnchorPane implements Initializable {
    public LoginView() {
        //this.setSpacing(10);
        Label lblUser = new Label("Tên người dùng:");
        TextField txtUser = new TextField();
        AnchorPane.setTopAnchor(lblUser, 20.0);
        AnchorPane.setLeftAnchor(lblUser, 50.0);
        AnchorPane.setTopAnchor(txtUser, 20.0);
        AnchorPane.setLeftAnchor(txtUser, 150.0);

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

    @FXML
    private ComboBox<String> roleComboBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Thêm các vai trò vào ComboBox khi màn hình vừa mở lên
        roleComboBox.getItems().addAll("Quản trị viên", "Người bán", "Người mua");
        // Đặt giá trị mặc định để không bị trống
        roleComboBox.setValue("Người mua");
    }

    @FXML
    public void handleLogin(javafx.event.ActionEvent actionEvent) throws IOException {
        String selectedRole = roleComboBox.getValue();

        if (selectedRole.equals("Quản trị viên")) {
            System.out.println("Đang đăng nhập quyền Admin...");
            // Chuyển sang file admin.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/com/uet/auction/view/admin.fxml"));
            Stage stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } else if (selectedRole.equals("Người bán")) {
            System.out.println("Đang đăng nhập quyền Seller...");
            // Chuyển sang file seller.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/com/uet/auction/view/seller.fxml"));
            Stage stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } else {
            System.out.println("Đang đăng nhập quyền User...");
            // Chuyển sang file user.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/com/uet/auction/view/user.fxml"));
            Stage stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
    }
}

