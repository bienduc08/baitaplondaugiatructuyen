package com.uet.auction.controller;

import com.uet.auction.model.Product;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;

import java.awt.*;
import java.time.LocalDateTime;

public class AdminView {
    private User user;
    private DataStorage dataStorage;
    private AuctionService auctionService;
    @FXML private Button btnApprove;
    @FXML private TableView<Product> tablePending; // Đảm bảo kiểu dữ liệu là <Product>
    @FXML private TextField txtReason;
    @FXML private DatePicker datePickerStart;
    @FXML private DatePicker datePickerEnd;

    public AdminView(User user, DataStorage dataStorage, AuctionService auctionService) {
        this.user = user;
        this.dataStorage = dataStorage;
        this.auctionService = auctionService;

    }
    // Constructor để nhận các đối tượng quản lý dữ liệu
    public AdminView(DataStorage dataStorage, AuctionService auctionService) {
        this.dataStorage = dataStorage;
        this.auctionService = auctionService;
    }
    // Logic trong AdminView hoặc AdminController
    public void updateAuctionTime(int productId, LocalDateTime start, LocalDateTime end) {
        // 1. Tìm sản phẩm trong kho chính thức
        Product p = dataStorage.getProductById(productId);

        if (p != null) {
            // 2. Cập nhật giờ mới
            p.setStartTime(start);
            p.setEndTime(end);

            // 3. Thông báo cập nhật tới tất cả người dùng
            auctionService.notifyAllUsers("Cập nhật: Sản phẩm " + p.getName() + " thay đổi thời gian: " + start.toString() + " -> " + end.toString());
        }
    }
    // Trong AdminView.java hoặc AdminController.java
    public void handleProductApproval(Product p, boolean isApproved, String reason) {
        if (isApproved) {
            // 1. Thiết lập thời gian mặc định (ví dụ: bắt đầu ngay, kết thúc sau 24h)
            p.setStartTime(LocalDateTime.now());
            p.setEndTime(LocalDateTime.now().plusHours(24));
            p.setActive(true);

            // 2. Cập nhật kho dữ liệu
            dataStorage.getPendingProducts().remove(p);
            dataStorage.getAllProducts().add(p);

            // 3. Thông báo cho mọi người
            auctionService.notifyAllUsers("Sản phẩm mới " + p.getName() + " đã được duyệt lên sàn!");
        } else {
            // 1. Xóa khỏi danh sách chờ
            dataStorage.getPendingProducts().remove(p);

            // 2. Thông báo riêng cho Seller (Người sở hữu sản phẩm)
            auctionService.notify(p.getOwner(), "Sản phẩm '" + p.getName() + "' bị từ chối. Lý do: " + reason);
        }
    }

    @FXML
    public void initialize() {
        btnApprove.setOnAction(e -> {
            Product selectedProduct = tablePending.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                // Gọi lệnh 1: Duyệt sản phẩm
                handleProductApproval(selectedProduct, true, "");

                tablePending.getItems().remove(selectedProduct);
                new Alert(Alert.AlertType.INFORMATION, "Phê duyệt thành công!").show();
            } else {
                new Alert(Alert.AlertType.WARNING, "Vui lòng chọn một sản phẩm để duyệt!").show();
            }
        });
    }
    public void show() {
    }


    public void onRejectButtonClick() {
        Product selectedProduct = tablePending.getSelectionModel().getSelectedItem();
        String lyDo = txtReason.getText();

        if (selectedProduct != null) {
            // Gọi service xử lý (Hàm này phải public trong AuctionService)
            auctionService.processApproval(selectedProduct, false, lyDo);
        }
    }

    public void onSaveTimeButtonClick() {
        Product selectedProduct = tablePending.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // Lưu ý: JavaFX DatePicker dùng getValue(), không phải getDateTimeValue()
            // Bạn cần convert LocalDate sang LocalDateTime
            LocalDateTime start = datePickerStart.getValue().atStartOfDay();
            LocalDateTime end = datePickerEnd.getValue().atTime(23, 59);

            auctionService.updateAuctionTime(selectedProduct.getId(), start, end);
        }
    }
}
