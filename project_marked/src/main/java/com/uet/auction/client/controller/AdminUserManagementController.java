package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.DTO.UserDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional; // [THÊM MỚI] dùng cho showConfirm

public class AdminUserManagementController {

    @FXML private TextField txtSearchUser;
    @FXML private TableView<UserDTO> tbvUsers;
    @FXML private TableColumn<UserDTO, Integer> colUserId;
    @FXML private TableColumn<UserDTO, String>  colUsername;
    @FXML private TableColumn<UserDTO, String>  colRole;
    @FXML private TableColumn<UserDTO, Double>  colBalance;
    @FXML private TableColumn<UserDTO, String>  colStatus;
    @FXML private Label lblUserCount; // [THÊM MỚI] label đếm tổng số user, khai báo trong FXML mới

    // [SỬA] File gốc dùng @FXML public static, bỏ @FXML vì instance không phải FXML field
    public static AdminUserManagementController instance;

    private final ObservableList<UserDTO> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;

        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // [THÊM MỚI] Badge màu cho cột Vai trò
        // File gốc chỉ dùng PropertyValueFactory đơn giản, không có custom cell factory
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setText(null); setStyle(""); return; }
                switch (role.toUpperCase()) {
                    case "ADMIN":
                        setText("Admin");
                        setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
                        break;
                    case "SELLER":
                        setText("Seller");
                        setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                        break;
                    case "BIDDER":
                        setText("Bidder");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        break;
                    default:
                        setText(role);
                        setStyle("");
                }
            }
        });

        // [THÊM MỚI] Định dạng tiền tệ cho cột Số dư
        colBalance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                setText(empty || balance == null ? null : String.format("%,.0f VNĐ", balance));
            }
        });

        // [THÊM MỚI] Màu sắc cho cột Trạng thái
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                switch (status.toUpperCase()) {
                    case "ACTIVE":
                        setText("Hoạt động");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        break;
                    case "LOCKED":
                        setText("Đã khóa");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        break;
                    default:
                        setText(status);
                        setStyle("-fx-text-fill: #7f8c8d;");
                }
            }
        });
        // [KẾT THÚC THÊM MỚI - cell factories]

        tbvUsers.setItems(userList);
        loadAllUsers();
    }

    // Hàm này KHÔNG thay đổi so với file gốc
    private void loadAllUsers() {
        userList.clear();
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_USERS", null));
    }

    // [THÊM MỚI] reloadUsers() - file gốc không có
    // Được ResponseListener gọi sau khi LOCK/UNLOCK thành công để tự refresh bảng
    public void reloadUsers() {
        String keyword = txtSearchUser.getText().trim();
        if (keyword.isEmpty()) {
            loadAllUsers();
        } else {
            userList.clear();
            SocketClient.sendRequest(new AuctionRequest("SEARCH_USER", keyword));
        }
    }

    // Hàm này KHÔNG thay đổi logic so với file gốc
    @FXML
    public void onSearchClick() {
        String keyword = txtSearchUser.getText().trim();
        if (keyword.isEmpty()) {
            loadAllUsers();
            return;
        }
        userList.clear();
        SocketClient.sendRequest(new AuctionRequest("SEARCH_USER", keyword));
    }

    // [THÊM MỚI] onRefreshClick() - file gốc không có nút này
    @FXML
    public void onRefreshClick() {
        txtSearchUser.clear();
        loadAllUsers();
    }

    @FXML
    public void onLockUserClick() {
        // [SỬA] File gốc gọi thẳng changeUserStatusAction() dùng chung
        // Tách ra để thêm kiểm tra: không khóa Admin, không khóa tài khoản đã khóa, hỏi xác nhận
        UserDTO selected = getSelectedUser();
        if (selected == null) return;

        // [THÊM MỚI] 2 kiểm tra bên dưới - file gốc không có
        if ("LOCKED".equalsIgnoreCase(selected.getStatus())) {
            AlertHelper.showError("Tài khoản này đã bị khóa rồi!");
            return;
        }
        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            AlertHelper.showError("Không thể khóa tài khoản Admin!");
            return;
        }

        // [THÊM MỚI] Hộp thoại xác nhận - file gốc gửi request luôn không hỏi
        Optional<ButtonType> confirm = AlertHelper.showConfirm(
            "Xác nhận khóa tài khoản",
            "Bạn có chắc muốn khóa tài khoản \"" + selected.getUsername() + "\" không?"
        );
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            SocketClient.sendRequest(new AuctionRequest("LOCK_USER", selected));
        }
    }

    @FXML
    public void onUnlockUserClick() {
        // [SỬA] Tương tự onLockUserClick - tách ra, thêm kiểm tra và xác nhận
        UserDTO selected = getSelectedUser();
        if (selected == null) return;

        // [THÊM MỚI] kiểm tra trạng thái trước
        if ("ACTIVE".equalsIgnoreCase(selected.getStatus())) {
            AlertHelper.showError("Tài khoản này đang hoạt động bình thường!");
            return;
        }

        // [THÊM MỚI] Hộp thoại xác nhận
        Optional<ButtonType> confirm = AlertHelper.showConfirm(
            "Xác nhận mở khóa",
            "Bạn có chắc muốn mở khóa tài khoản \"" + selected.getUsername() + "\" không?"
        );
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            SocketClient.sendRequest(new AuctionRequest("UNLOCK_USER", selected));
        }
    }

    // [THÊM MỚI] Tách getSelectedUser() thành hàm riêng cho gọn
    // File gốc lặp lại tbvUsers.getSelectionModel().getSelectedItem() trong cả 2 hàm
    private UserDTO getSelectedUser() {
        UserDTO selected = tbvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Vui lòng chọn một người dùng từ danh sách trước!");
        }
        return selected;
    }

    // [SỬA] File gốc không cập nhật lblUserCount (vì chưa có label đó)
    // Thêm dòng cập nhật đếm số user
    public void updateTableData(List<UserDTO> users) {
        if (users != null) {
            userList.setAll(users);
            if (lblUserCount != null) { // [THÊM MỚI] cập nhật label đếm
                lblUserCount.setText("Tổng: " + users.size() + " người dùng");
            }
        }
    }
}
