package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.uet.auction.client.controller.UserController.onBackAction;

public class BidHistoryController {

    public static Runnable onBackAction;
    @FXML private Label productNameLabel;
    @FXML private Label totalBidsLabel;

    // Bảng lịch sử
    @FXML private TableView<BidDTO> bidHistoryTable;
    @FXML private TableColumn<BidDTO, String> timeCol;
    @FXML private TableColumn<BidDTO, String> bidderCol;
    @FXML private TableColumn<BidDTO, BigDecimal> priceCol;
    @FXML private TableColumn<BidDTO, String> statusCol;

    // Biểu đồ LineChart
    @FXML private LineChart<String, Number> bidLineChart;
    @FXML private NumberAxis yAxis;

    private final ObservableList<BidDTO> bidListData = FXCollections.observableArrayList();
    private Integer currentProductId;
    private Timeline autoRefreshTimeline;

    public static BidHistoryController instance;

    @FXML
    public void initialize() {
        instance = this;

        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        bidderCol.setCellValueFactory(new PropertyValueFactory<>("biddername"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format cột giá
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); return; }
                setText(String.format("%,.0f VNĐ", price));
                setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
            }
        });

        bidHistoryTable.setItems(bidListData);

        // Cấu hình biểu đồ
        if (bidLineChart != null) {
            bidLineChart.setAnimated(false);
            bidLineChart.setCreateSymbols(true);
            bidLineChart.setLegendVisible(true);
            bidLineChart.setPrefHeight(380);
        }
        if (yAxis != null) {
            yAxis.setAutoRanging(true);
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override public String toString(Number value) {
                    double v = value.doubleValue();
                    if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
                    if (v >= 1_000)    return String.format("%.0fK", v / 1_000);
                    return String.format("%.0f", v);
                }
            });
        }

        // Tự động làm mới mỗi 3 giây
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> loadBidHistory(true)));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        bidHistoryTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) autoRefreshTimeline.play();
            else                  autoRefreshTimeline.stop();
        });
    }

    public void setProductContext(Integer productId, String productName) {
        this.currentProductId = productId;
        productNameLabel.setText("Tên sản phẩm: " + productName);
        loadBidHistory(false);
    }

    private void loadBidHistory() { loadBidHistory(false); }

    private void loadBidHistory(boolean isAutoRefresh) {
        if (currentProductId == null) return;
        if (!isAutoRefresh) {
            bidListData.clear();
            totalBidsLabel.setText("Đang tải...");
        }
        SocketClient.sendRequest(new AuctionRequest("GET_BID_HISTORY", currentProductId));
    }

    /**
     * Được gọi bởi ResponseListener khi Server trả về dữ liệu lịch sử.
     * Cập nhật cả bảng lẫn biểu đồ LineChart.
     */
    public void displayBidHistory(List<BidDTO> list) {
        Platform.runLater(() -> {
            if (list == null) return;

            // Kiểm tra nếu dữ liệu không đổi thì không redraw
            boolean isIdentical = bidListData.size() == list.size();
            if (isIdentical) {
                for (int i = 0; i < list.size(); i++) {
                    BidDTO b1 = bidListData.get(i);
                    BidDTO b2 = list.get(i);
                    if (!Objects.equals(b1.getId(), b2.getId()) ||
                            !Objects.equals(b1.getPrice(), b2.getPrice()) ||
                            !Objects.equals(b1.getBiddername(), b2.getBiddername())) {
                        isIdentical = false;
                        break;
                    }
                }
            }

            if (!isIdentical) {
                bidListData.setAll(list);
                updateChart(list);
            }
            totalBidsLabel.setText("Tổng cộng: " + list.size() + " lượt trả giá");
        });
    }

    /**
     * Vẽ lại LineChart từ danh sách bid (sắp xếp theo thời gian tăng dần).
     */
    private void updateChart(List<BidDTO> list) {
        if (bidLineChart == null || list == null || list.isEmpty()) return;

        // Sắp xếp tăng dần theo Giá tiền (đảm bảo biểu đồ luôn đi lên)
        List<BidDTO> sorted = new ArrayList<>(list);
        // Sort theo thời gian tăng dần để biểu đồ phản ánh diễn biến thực
        sorted.sort((a, b) -> {
            if (a.getTime() == null) return -1;
            if (b.getTime() == null) return 1;
            return a.getTime().compareTo(b.getTime());
        });

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Diễn biến giá");

        for (int i = 0; i < sorted.size(); i++) {
            BidDTO bid = sorted.get(i);
            if (bid.getPrice() == null) continue;

            // Nhãn trục X: "Lượt N\nNgười"
            String label = "Lượt " + (i + 1);
            XYChart.Data<String, Number> point = new XYChart.Data<>(label, bid.getPrice());

            // Tooltip khi hover
            Tooltip tooltip = new Tooltip(
                    String.format("Người: %s%nGiá: %,.0f VNĐ%nThời gian: %s",
                            bid.getBiddername() != null ? bid.getBiddername() : "—",
                            bid.getPrice(),
                            bid.getTime() != null ? bid.getTime() : "—")
            );
            tooltip.setShowDelay(Duration.millis(100));

            // Gắn tooltip vào node sau khi chart render xong
            point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tooltip);
                    newNode.setStyle("-fx-background-color: #2980b9, white;");
                }
            });

            series.getData().add(point);
        }

        bidLineChart.getData().clear();
        bidLineChart.getData().add(series);

        // Style đường kẻ
        Platform.runLater(() -> {
            if (!series.getData().isEmpty() && series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #2980b9; -fx-stroke-width: 2.5px;");
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        loadBidHistory(); }

    @FXML
    private void onCloseClick() {
        instance = null;

        // Dừng vòng lặp tự động làm mới để tránh lỗi tràn bộ nhớ (memory leak)
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        // Kích hoạt hàm quay lại giao diện trước
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}