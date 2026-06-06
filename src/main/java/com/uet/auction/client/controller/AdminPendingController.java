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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPendingController {

    public static AdminPendingController instance;
    @FXML private TableView<ProductDTO> pendingTable;
    @FXML private TableColumn<ProductDTO, Integer> idCol;
    @FXML private TableColumn<ProductDTO, String>  nameCol;
    @FXML private TableColumn<ProductDTO, Double>  priceCol;
    @FXML private TableColumn<ProductDTO, Double>  currentCol;
    @FXML private TableColumn<ProductDTO, String>  sellerCol;
    @FXML private TableColumn<ProductDTO, Double>  stepCol;
    @FXML private TableColumn<ProductDTO, LocalDateTime> startTimeCol;
    @FXML private TableColumn<ProductDTO, LocalDateTime> endTimeCol;
    @FXML private TableColumn<ProductDTO, String>  statusCol;

    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        pendingTable.setItems(pendingListData);

        loadPendingProducts();

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshPendingProducts()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        if (pendingTable.getScene() != null) autoRefreshTimeline.play();
        pendingTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) autoRefreshTimeline.play();
            else autoRefreshTimeline.stop();
        });
    }

    private void setupTable() {
        if (idCol != null) idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (sellerCol != null) sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        if (priceCol != null) {
            priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
            priceCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (currentCol != null) {
            currentCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            currentCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (stepCol != null){
            stepCol.setCellValueFactory(new PropertyValueFactory<>("stepPrice"));
            stepCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
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

    /** * Đã SỬA: Giữ lại vùng chọn của người dùng khi bảng tự động refresh
     */
    public void updateTableData(List<ProductDTO> products) {
        Platform.runLater(() -> {
            // Lấy ra ID của sản phẩm đang được chọn
            ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
            int selectedId = (selected != null) ? selected.getId() : -1;

            // Nạp dữ liệu mới
            pendingListData.setAll(products);

            // Bôi đen lại đúng sản phẩm đó nếu nó vẫn còn trên bảng
            if (selectedId != -1) {
                for (ProductDTO p : pendingListData) {
                    if (p.getId() == selectedId) {
                        pendingTable.getSelectionModel().select(p);
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
     * Đã SỬA: Sử dụng APPROVE_PRODUCT truyền toàn bộ Object để Server lưu thông báo
     */
    @FXML
    public void onApproveButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Vui lòng chọn sản phẩm trên bảng để duyệt!");
            return;
        }
        if (!"PENDING".equals(selected.getStatus())) {
            AlertHelper.showError("Chỉ có thể duyệt sản phẩm đang ở trạng thái 'Chờ duyệt'!");
            return;
        }

        // Gửi toàn bộ đối tượng ProductDTO lên
        SocketClient.sendRequest(new AuctionRequest("APPROVE_PRODUCT", selected));
    }

    /**
     * Đã SỬA: Sử dụng REJECT_PRODUCT để báo tin buồn cho Seller
     */
    @FXML
    public void onRejectButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Vui lòng chọn sản phẩm trên bảng để từ chối!");
            return;
        }
        if (!"PENDING".equals(selected.getStatus())) {
            AlertHelper.showError("Chỉ có thể từ chối sản phẩm đang ở trạng thái 'Chờ duyệt'!");
            return;
        }

        SocketClient.sendRequest(new AuctionRequest("REJECT_PRODUCT", selected));
    }

    public void refreshPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }
}