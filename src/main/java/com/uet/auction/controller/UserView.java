package com.uet.auction.controller;

import com.uet.auction.model.Auction;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class UserView implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UserView.class);

    @FXML private Button            btnBid;
    @FXML private Button            btnRefresh;
    @FXML private ListView<Auction> listAuctions;
    @FXML private TextField         txtBidAmount;
    @FXML private Label             lblWelcome;
    @FXML private Label             lblStatus;

    private User              user;
    private AuctionController auctionController;
    private DataStorage       dataStorage;

    public void initData(User user, AuctionController auctionController, DataStorage dataStorage) {
        this.user              = user;
        this.auctionController = auctionController;
        this.dataStorage       = dataStorage;
        lblWelcome.setText("Chào " + user.getUsername()
                + " | Số dư: " + user.getBalance() + " đ");
        processRequest("USER_REFRESH");
        log.info("UserView initData: user={}", user.getUsername());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("UserView khởi tạo");
    }

    private void processRequest(String action) {
        try {
            switch (action) {
                case "USER_BID"     -> handleBid();
                case "USER_REFRESH" -> handleRefresh();
                default             -> log.warn("Action User không hợp lệ: {}", action);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý action User [{}]: {}", action, e.getMessage(), e);
            lblStatus.setText("Lỗi: " + e.getMessage());
        }
    }

    private void handleBid() {
        if (!user.canBid()) {
            new Alert(Alert.AlertType.ERROR, "Tài khoản chưa được cấp quyền đấu giá!").showAndWait();
            return;
        }
        Auction selected = listAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn phiên đấu giá!").showAndWait();
            return;
        }
        if (selected.getEndTime() != null && LocalDateTime.now().isAfter(selected.getEndTime())) {
            new Alert(Alert.AlertType.WARNING, "Phiên đấu giá đã kết thúc!").showAndWait();
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(txtBidAmount.getText().trim());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Số tiền không hợp lệ!").showAndWait();
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            new Alert(Alert.AlertType.WARNING, "Số tiền phải lớn hơn 0!").showAndWait();
            return;
        }
        log.info("Đặt giá: user={}, auctionId={}, amount={}",
                user.getUsername(), selected.getId(), amount);

        BidRequest request = new BidRequest(user, selected.getId(), amount);
        boolean result = (boolean) auctionController.processRequest("BID_PLACE", request);

        if (result) {
            lblStatus.setText("Đặt giá thành công: " + amount + " đ");
            txtBidAmount.clear();
            log.info("Đặt giá thành công: user={}, amount={}", user.getUsername(), amount);
        } else {
            lblStatus.setText("Đặt giá thất bại — giá quá thấp hoặc không đủ số dư.");
            log.warn("Đặt giá thất bại: user={}, amount={}", user.getUsername(), amount);
        }
    }

    private void handleRefresh() {
        listAuctions.setItems(FXCollections.observableArrayList(
                dataStorage.getActiveAuctions()));
        log.info("Làm mới danh sách: user={}", user != null ? user.getUsername() : "chưa init");
    }

    @FXML public void onBidButtonClick()     { processRequest("USER_BID"); }
    @FXML public void onRefreshButtonClick() { processRequest("USER_REFRESH"); }
}