package com.uet.auction;

import com.uet.auction.controller.LoginView;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * AuctionApplication — điểm khởi động duy nhất của toàn bộ ứng dụng.
 *
 * Luồng chạy:
 *   main()
 *     → launch() [JavaFX runtime]
 *       → init()  : khởi tạo DataStorage + AuctionService một lần duy nhất
 *       → start() : load login.fxml, gắn controller, hiển thị cửa sổ
 *       → stop()  : dọn dẹp khi người dùng đóng app
 *
 * Lý do toàn bộ file ghép lại vẫn chạy được:
 *   - Không có dependency bên ngoài: không Spring, không DB driver, không config file
 *   - DataStorage dùng List in-memory → chạy ngay không cần kết nối
 *   - AuctionService chỉ gọi DataStorage → không có network call
 *   - JavaFX chỉ cần FXMLLoader khớp fx:controller với tên class → compile và chạy ngay
 */
public class AuctionApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(AuctionApplication.class);

    // Khởi tạo một lần, truyền xuống toàn bộ View qua initData()
    private DataStorage    dataStorage;
    private AuctionService auctionService;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * init() chạy trước start() trên JavaFX Application Thread.
     * Dùng để khởi tạo các service — tránh làm nặng start().
     */
    @Override
    public void init() {
        log.info("Khởi tạo DataStorage và AuctionService...");
        dataStorage    = new DataStorage();
        auctionService = new AuctionService(dataStorage);
        log.info("Khởi tạo xong");
    }

    /**
     * start() — load giao diện và hiển thị cửa sổ chính.
     *
     * Dùng loader.setController() thay vì để FXMLLoader tự tạo controller
     * → giúp truyền dataStorage và auctionService vào LoginView ngay từ đầu
     * → LoginView truyền tiếp xuống AdminView / SellerView / UserView qua initData()
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        log.info("Khởi động giao diện...");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/uet/auction/view/login.fxml"));

        // Gắn controller thủ công để truyền dependency
        LoginView loginController = new LoginView(dataStorage, auctionService);
        loader.setController(loginController);

        Scene scene = new Scene(loader.load(), 400, 320);

        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - UET");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        log.info("Ứng dụng sẵn sàng");
    }

    /**
     * stop() — chạy khi người dùng đóng cửa sổ.
     * Dùng để giải phóng tài nguyên nếu sau này thêm DB hoặc socket.
     */
    @Override
    public void stop() {
        log.info("Tắt ứng dụng");
    }
}