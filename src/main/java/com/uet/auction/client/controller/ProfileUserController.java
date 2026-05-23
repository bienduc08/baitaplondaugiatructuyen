package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileUserController {

    public static ProfileUserController instance;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderEmail;
    @FXML private Label lblTotalBids;
    @FXML private Label lblTotalWins;

    @FXML private Button btnDeposit;

    @FXML private TableView<BidDTO>              tblMyBids;
    @FXML private TableColumn<BidDTO, Integer>   colProduct;
    @FXML private TableColumn<BidDTO, Double>    colAmount;
    @FXML private TableColumn<BidDTO, String>    colTime;
    @FXML private TableColumn<BidDTO, String>    colStatus;

    private final ObservableList<BidDTO> bidList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;

        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            // Tên hiển thị chính
            String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                    ? user.getFullName() : user.getUsername();
            if (lblUsername != null) lblUsername.setText(displayName);
            if (lblRole != null)    lblRole.setText(roleDisplay(user.getRole()));
            if (lblBalance != null)
                lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));

            // Góc trên phải: Họ tên và email từ đăng ký
            if (lblHeaderName != null) {
                String fullName = (user.getFullName() != null && !user.getFullName().isEmpty())
                        ? user.getFullName() : user.getUsername();
                lblHeaderName.setText("👤 " + fullName);
            }
            if (lblHeaderEmail != null) {
                String gmail = (user.getGmail() != null && !user.getGmail().isEmpty())
                        ? user.getGmail() : user.getUsername() + "@gmail.com";
                lblHeaderEmail.setText("✉ " + gmail);
            }

            // Tải dữ liệu từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
                SocketClient.sendRequest(new AuctionRequest("GET_MY_BIDS", username));
            }
        }

        setupTable();
        if (tblMyBids != null) tblMyBids.setItems(bidList);
    }

    private void setupTable() {
        if (colProduct != null) {
            colProduct.setCellValueFactory(new PropertyValueFactory<>("productId"));
            colProduct.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Integer id, boolean empty) {
                    super.updateItem(id, empty);
                    setText(empty || id == null ? null : "SP #" + id);
                }
            });
        }
        if (colAmount != null) {
            colAmount.setCellValueFactory(new PropertyValueFactory<>("price"));
            colAmount.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (colTime   != null) colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    setText(s);
                    setStyle("Hợp lệ".equals(s)
                            ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                            : "-fx-text-fill: #e74c3c;");
                }
            });
        }
    }

    public void displayMyBids(List<BidDTO> bids) {
        Platform.runLater(() -> {
            bidList.setAll(bids != null ? bids : List.of());
            updateStats(bids);
        });
    }

    private void updateStats(List<BidDTO> bids) {
        if (bids == null || bids.isEmpty()) {
            if (lblTotalBids != null) lblTotalBids.setText("0");
            if (lblTotalWins != null) lblTotalWins.setText("0");
            return;
        }
        if (lblTotalBids != null) lblTotalBids.setText(String.valueOf(bids.size()));
        Set<Integer> productIds = new HashSet<>();
        for (BidDTO b : bids) {
            if (b.getProductId() != null) productIds.add(b.getProductId());
        }
        if (lblTotalWins != null) lblTotalWins.setText(String.valueOf(productIds.size()));
    }


    public void updateBalance() {
        UserDTO user = SessionManager.getCurrentUser();
        if (user != null && lblBalance != null)
            lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));
    }

    @FXML
    public void onDepositClick() {
        String username = SessionManager.getCurrentUsername();
        if (username == null) { AlertHelper.showError("Phiên đăng nhập không hợp lệ!"); return; }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nạp tiền vào ví");
        dialog.setContentText("Nhập số tiền (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            String raw = input.trim().replace(",", "").replace(".", "");
            if (raw.isEmpty()) { AlertHelper.showError("Vui lòng nhập số tiền!"); return; }
            double amount;
            try { amount = Double.parseDouble(raw); }
            catch (NumberFormatException e) { AlertHelper.showError("Số tiền không hợp lệ!"); return; }
            if (amount <= 0) { AlertHelper.showError("Số tiền nạp phải lớn hơn 0!"); return; }
            if (amount > 500_000_000) { AlertHelper.showError("Mỗi lần nạp tối đa 500.000.000 VNĐ!"); return; }
            if (btnDeposit != null) btnDeposit.setDisable(true);
            SocketClient.sendRequest(new AuctionRequest("DEPOSIT", new Object[]{username, amount}));
        });
    }

    public void handleDepositSuccess(double newBalance) {
        Platform.runLater(() -> {
            UserDTO user = SessionManager.getCurrentUser();
            if (user != null) user.setBalance(newBalance);
            updateBalance();
            if (btnDeposit != null) btnDeposit.setDisable(false);
        });
    }

    public void handleDepositFailure() {
        if (btnDeposit != null) btnDeposit.setDisable(false);
    }


    private String roleDisplay(String role) {
        if (role == null) return "—";
        switch (role) {
            case "SELLER": return "🏪 Người bán";
            case "BIDDER": return "🔨 Người đấu giá";
            case "USER":   return "👤 Người dùng";
            default:       return "👤 Người dùng";
        }
    }

    @FXML
    public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void onBackButtonClick() {
        try {
            UserDTO user = SessionManager.getCurrentUser();
            if (user != null && "SELLER".equals(user.getRole())) {
                SceneManager.switchScene("/com/uet/auction/view/Seller.fxml", "Người bán");
            } else {
                SceneManager.switchScene("/com/uet/auction/view/User.fxml", "Trang chủ");
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
