package com.uet.auction.controller;

import com.uet.auction.repository.DataStorage;
import com.uet.auction.model.Product;
import com.uet.auction.model.User;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.management.openmbean.CompositeData;
import java.time.LocalDateTime;
import java.util.List;

import static javafx.scene.control.Alert.*;

public class UserView {
    private User user;
    private Product product;
    private Stage stage;
    private Scene scene;
    private DataStorage dataStorage;

    public UserView(User user, Product product) {
        this.user = user;
        this.product = product;
        this.stage = new Stage();
        //Giao diện
        VBox layout = new VBox(10);
        layout.getChildren().add(new Label("Chào Người mua: " + user.getUsername()));

        Scene scene = new Scene(layout, 400, 300);
        stage.setScene(scene);
        stage.setTitle("Giao diện Đấu giá - User");


        Button btnBid = new Button("Đặt giá");
        layout.getChildren().add(btnBid);
        // Trong UserView.java
        btnBid.setOnAction(e -> {
            if (this.user.getRole() == User.Role.USER) {
                handleBidLogic();
                // Thực hiện đặt giá
            } else {
                Alert alert = new Alert(AlertType.ERROR, "Bạn không có quyền");
                alert.showAndWait();
            }
        });
    }

    public void handleBidLogic() {
        LocalDateTime now = LocalDateTime.now();
        if (product != null && now.isAfter(product.getEndTime())) {
            System.out.println("Phiên đấu giá đã kết thúc!");
            return;
        }
    }// Gọi service để đặt giá ở đây

    public void show() {
        if (stage != null) {
            stage.show();
        }
    }

    public void refreshData() {
        List<Product> latestProducts = dataStorage.getAll();

        // 2. Vẽ lại cái bảng (Table) hoặc danh sách trên màn hình
        // Nếu dùng JavaFX: myTable.setItems(FXCollections.observableArrayList(latestProducts));

        System.out.println("Đã cập nhật dữ liệu mới nhất từ Admin/Seller!");
    }
}
