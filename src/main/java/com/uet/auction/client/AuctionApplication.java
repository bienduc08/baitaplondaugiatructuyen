package com.uet.auction.client;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuctionApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // SỬA: Đăng ký stage vào SceneManager TRƯỚC KHI chuyển màn hình
        SceneManager.setPrimaryStage(stage);

        // Kết nối với Server
        SocketClient.connect();

        // Hiện màn hình Login
        FXMLLoader fxmlLoader = new FXMLLoader(
                AuctionApplication.class.getResource("/com/uet/auction/view/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến UET");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}