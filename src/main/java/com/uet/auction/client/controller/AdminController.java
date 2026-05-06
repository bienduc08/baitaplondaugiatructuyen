package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;

public class AdminController {

    // Các trường dùng để thêm sản phẩm mới (Nhớ đặt fx:id trong file admin.fxml cho khớp)
    @FXML private TextField productNameField;
    @FXML private TextField startingPriceField;
    // Tạm dùng TextField cho số ngày đấu giá cho đơn giản, hoặc bạn có thể dùng DatePicker
    @FXML private TextField durationDaysField;

    // Nơi hiển thị danh sách sản phẩm (nếu Admin cũng cần xem)
    @FXML private VBox productListContainer;

    // Để ResponseListener gọi ngược lại
    public static AdminController instance;

    @FXML
    public void initialize() {
        instance = this;
        // Có thể gọi hàm load danh sách sản phẩm ở đây nếu admin.fxml có chỗ hiển thị
        // loadAllProducts();
    }

    // Sự kiện khi Admin bấm nút "Thêm sản phẩm"
    @FXML
    public void onAddProductClick() {
        String name = productNameField.getText();
        String priceStr = startingPriceField.getText();
        String daysStr = durationDaysField.getText();

        if (name.isEmpty() || priceStr.isEmpty() || daysStr.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập đầy đủ thông tin sản phẩm!");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr);
            int days = Integer.parseInt(daysStr);

            // Gói dữ liệu gửi đi (Gồm Tên, Giá khởi điểm, và Số ngày đấu giá)
            Object[] productData = new Object[]{name, startingPrice, days};
            AuctionRequest request = new AuctionRequest("ADD_PRODUCT", productData);

            // Gửi lên Server
            SocketClient.sendRequest(request);

        } catch (NumberFormatException e) {
            AlertHelper.showError("Giá tiền và số ngày phải là định dạng số!");
        }
    }

    // Hàm nhận phản hồi từ Server (Được gọi bởi ResponseListener)
    public void handleAdminResponse(String type, boolean success, String message) {

                AlertHelper.showInfo("Thành công: " + message);
                // Xóa trắng các ô nhập liệu sau khi thêm xong
                productNameField.clear();
                startingPriceField.clear();
                durationDaysField.clear();
            } else {
                AlertHelper.showError("Lỗi: " + message);
            }
        });
    }

    // Sự kiện khi bấm Đăng xuất
    @FXML
    public void onLogoutClick() {
        Stage stage = (Stage) productNameField.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/login.fxml");
    }
    // Gọi hàm này khi Admin bấm nút DUYỆT trên một sản phẩm
    public void onApproveProduct(int productId) {
        // Gửi mảng chứa ID sản phẩm và trạng thái mới
        Object[] approveData = new Object[]{productId, "APPROVED"};
        AuctionRequest request = new AuctionRequest("CHANGE_PRODUCT_STATUS", approveData);
        SocketClient.sendRequest(request);
    }

    // Gọi hàm này khi Admin bấm nút TỪ CHỐI
    public void onRejectProduct(int productId) {
        Object[] rejectData = new Object[]{productId, "REJECTED"};
        AuctionRequest request = new AuctionRequest("CHANGE_PRODUCT_STATUS", rejectData);
        SocketClient.sendRequest(request);
    }
    public void loadPendingProducts() {
        AuctionRequest req = new AuctionRequest("GET_PENDING_PRODUCTS", null);
        SocketClient.sendRequest(req);
    }

// Bổ sung vào ResponseListener (ở Client) để bắt kết quả:
// case "GET_PRODUCTS_RESULT":
//    Nếu là AdminController.instance thì lấy list PENDING và vẽ ra màn hình.
//    Nếu là UserController.instance thì lấy list OPEN và vẽ ra màn hình.
}
