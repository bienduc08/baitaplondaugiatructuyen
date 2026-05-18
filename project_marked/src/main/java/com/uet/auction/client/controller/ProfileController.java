package com.uet.auction.client.controller;

import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.UserDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;

public class ProfileController {

    public static ProfileController instance;
    public static Runnable onBackAction;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblTotalBids;

    @FXML private TableView<BidDTO>              tblMyBids;
    @FXML private TableColumn<BidDTO, String>    colProduct;
    @FXML private TableColumn<BidDTO, Double>    colAmount;
    @FXML private TableColumn<BidDTO, String>    colTime;
    @FXML private TableColumn<BidDTO, String>    colStatus;

    private final ObservableList<BidDTO> bidList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;

        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            lblUsername.setText(user.getUsername());
            lblRole.setText(roleDisplay(user.getRole()));
            lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));
        }

        setupTable();
        tblMyBids.setItems(bidList);
    }

    private void setupTable() {
        if (colProduct != null) colProduct.setCellValueFactory(new PropertyValueFactory<>("productId"));
        if (colAmount  != null) {
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

    private String roleDisplay(String role) {
        if (role == null) return "—";
        switch (role) {
            case "ADMIN":  return "🛡 Quản trị viên";
            case "SELLER": return "🏪 Người bán";
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
        if (onBackAction != null) {
            onBackAction.run();
        } else {
            fallbackBackAction();
        }
    }

    private void fallbackBackAction() {
        try {
            UserDTO user = SessionManager.getCurrentUser();
            if (user == null) {
                SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
                return;
            }
            switch (user.getRole()) {
                case "ADMIN":
                    SceneManager.switchScene("/com/uet/auction/view/Admin.fxml", "Quản trị viên");
                    break;
                case "SELLER":
                    SceneManager.switchScene("/com/uet/auction/view/Seller.fxml", "Người bán");
                    break;
                default:
                    SceneManager.switchScene("/com/uet/auction/view/User.fxml", "Trang chủ");
                    break;
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}