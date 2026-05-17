package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller cho AdminPending.fxml
 * Quản lý danh sách sản phẩm ở trạng thái PENDING để Admin duyệt.
 */
public class AdminPendingController {

    @FXML private TableView<ProductDTO> tbvPendingProducts;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String> colName;
    @FXML private TableColumn<ProductDTO, String> colSeller;
    @FXML private TableColumn<ProductDTO, Double> colPrice;
    @FXML private TableColumn<ProductDTO, LocalDateTime> colStartTime;
    @FXML private TableColumn<ProductDTO, LocalDateTime> colEndTime;
    @FXML public static AdminPendingController instance;

    private final ObservableList<ProductDTO> pendingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance =this;
        // Ánh xạ dữ liệu từ ProductDTO vào các cột tương ứng của TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        tbvPendingProducts.setItems(pendingList);

        // Gọi hàm tải danh sách ban đầu khi vừa vào tab này
        refreshPendingProducts();
    }

    /**
     * Gửi yêu cầu lên server để lấy danh sách sản phẩm đang chờ duyệt mới nhất
     */
    public void refreshPendingProducts() {
        pendingList.clear();
        // Server sẽ xử lý request này và trả về kết quả qua luồng đọc Socket (Network Thread)
        SocketClient.sendRequest(new AuctionRequest("GET_PENDING_PRODUCTS", null));
    }

    /**
     * Xử lý khi Admin nhấn nút "✅ Phê duyệt"
     */
    @FXML
    public void onApproveClick() {
        handleProductAction("APPROVE_PRODUCT", "Đã phê duyệt sản phẩm thành công!");
    }

    /**
     * Xử lý khi Admin nhấn nút "❌ Từ chối"
     */
    @FXML
    public void onRejectClick() {
        handleProductAction("REJECT_PRODUCT", "Đã từ chối phê duyệt sản phẩm này!");
    }

    /**
     * Hàm dùng chung xử lý hành động với dòng dữ liệu được chọn trên bảng
     */
    private void handleProductAction(String requestAction, String successMessage) {
        ProductDTO selectedProduct = tbvPendingProducts.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            AlertHelper.showError("Vui lòng chọn một sản phẩm trong danh sách trước!");
            return;
        }

        try {
            // Gửi đối tượng sản phẩm được chọn kèm hành động tương ứng lên server
            SocketClient.sendRequest(new AuctionRequest(requestAction, selectedProduct));
            AlertHelper.showInfo(successMessage);

            // Xóa tạm thời sản phẩm khỏi giao diện tại chỗ để không cần load lại toàn bộ bảng
            pendingList.remove(selectedProduct);
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Có lỗi xảy ra trong quá trình gửi yêu cầu xử lý!");
        }
    }

    /**
     * Hàm bổ trợ để cập nhật danh sách nhận về từ luồng Thread đọc Socket của Client
     */
    public void updateTableData(List<ProductDTO> products) {
        if (products != null) {
            pendingList.setAll(products);
        }
    }
}