package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller cho AdminPending.fxml
 * Quản lý danh sách sản phẩm ở trạng thái PENDING để Admin duyệt.
 */
public class AdminPendingController {

    public static AdminPendingController instance;
    @FXML private TableView<ProductDTO> pendingTable;
    @FXML private TableColumn<ProductDTO, Integer> idCol;
    @FXML private TableColumn<ProductDTO, String>  nameCol;
    @FXML private TableColumn<ProductDTO, BigDecimal>  priceCol;
    @FXML private TableColumn<ProductDTO, BigDecimal>  currentCol;
    @FXML private TableColumn<ProductDTO, String>  sellerCol;
    @FXML private TableColumn<ProductDTO, BigDecimal>  stepCol;
    @FXML private TableColumn<ProductDTO, LocalDateTime> startTimeCol;
    @FXML private TableColumn<ProductDTO, LocalDateTime> endTimeCol;
    @FXML private TableColumn<ProductDTO, String>  statusCol;

    // Sử dụng DUY NHẤT một danh sách này để map với TableView
    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        pendingTable.setItems(pendingListData);

        // Gọi hàm tải danh sách ban đầu khi vừa vào tab này
        loadPendingProducts();

        // Tự động làm mới mỗi 5 giây
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshPendingProducts()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        if (pendingTable.getScene() != null) {
            autoRefreshTimeline.play();
        }
        pendingTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                autoRefreshTimeline.play();
            } else {
                autoRefreshTimeline.stop();
            }
        });
    }

    private void setupTable() {
        if (idCol != null) idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (sellerCol != null) sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        if (priceCol != null) {
            priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
            priceCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (currentCol != null) {
            currentCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            currentCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (stepCol != null){
            stepCol.setCellValueFactory(new PropertyValueFactory<>("stepPrice"));
            stepCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }

        if (startTimeCol != null){
            startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
            startTimeCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? "—" : item.format(FMT));
                }
            });
        }
        if (endTimeCol != null) {
            endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            endTimeCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? "—" : item.format(FMT));
                }
            });
        }
        if (statusCol != null) {
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    switch (s) {
                        case "PENDING":  setText("⏳ Chờ duyệt"); setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); break;
                        case "OPEN":     setText("🔥 Đang đấu");  setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); break;
                        case "CLOSED":   setText("🔒 Đã đóng");   setStyle("-fx-text-fill: #7f8c8d;"); break;
                        case "REJECTED": setText("✘ Từ chối");    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                        default:         setText(s); setStyle(""); break;
                    }
                }
            });
        }
    }
    /**
     * Nhận danh sách từ Server và đổ dữ liệu lên TableView
     */
    public void updateTableData(List<ProductDTO> products) {
        Platform.runLater(() -> {
            // 1. Lưu lại ID của sản phẩm đang được chọn trước khi nạp lại
            ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
            int selectedId = selected != null ? selected.getId() : -1;

            // 2. Chỉ hiển thị các sản phẩm có trạng thái PENDING (chờ duyệt)
            java.util.List<ProductDTO> pendingProducts = products.stream()
                    .filter(p -> "PENDING".equals(p.getStatus()))
                    .toList();

            // 3. Đổ dữ liệu đã lọc vào bảng
            pendingListData.setAll(pendingProducts);

            // 4. Chọn lại sản phẩm cũ nếu nó vẫn còn trong danh sách chờ duyệt
            if (selectedId != -1) {
                for (int i = 0; i < pendingListData.size(); i++) {
                    if (pendingListData.get(i).getId() == selectedId) {
                        pendingTable.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        });
    }

    public void loadPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }

    /**
     * Cập nhật lại danh sách trên TableView (Được gọi từ Socket Reader khi Server trả dữ liệu)
     */

    @FXML
    public void onApproveButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Vui lòng chọn sản phẩm!"); return; }
        if ("OPEN".equals(selected.getStatus())) { AlertHelper.showError("Sản phẩm này đang được đấu giá!"); return; }
        if ("CLOSED".equals(selected.getStatus())) { AlertHelper.showError("Sản phẩm này đã đóng!"); return; }

        // Gửi APPROVE_PRODUCT để server set status APPROVED, Timer sẽ tự mở đúng giờ start_time
        // Không gửi CHANGE_PRODUCT_STATUS với "OPEN" vì sẽ bỏ qua start_time đã đặt
        SocketClient.sendRequest(new AuctionRequest("APPROVE_PRODUCT", selected));
    }

    @FXML
    public void onRejectButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Vui lòng chọn sản phẩm!"); return; }

        Object[] data = {selected.getId(), "REJECTED"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));

        // Cập nhật UI ngay lập tức
        pendingListData.remove(selected);
        AlertHelper.showInfo("Đã từ chối sản phẩm!");
    }

    /**
     * Gửi yêu cầu lên server để lấy danh sách sản phẩm đang chờ duyệt mới nhất
     */
    public void refreshPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }
}