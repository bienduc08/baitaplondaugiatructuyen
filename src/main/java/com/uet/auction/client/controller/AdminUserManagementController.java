package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.DTO.UserDTO; // Hãy đảm bảo lớp này tồn tại trong common DTO của bạn
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

/**
 * Controller cho AdminUserManagement.fxml
 * Cho phép Admin xem danh sách thành viên, tìm kiếm và thay đổi trạng thái (Khóa/Mở khóa).
 */
public class AdminUserManagementController {

    @FXML private TextField txtSearchUser;
    @FXML private TableView<UserDTO> tbvUsers;
    @FXML private TableColumn<UserDTO, Integer> colUserId;
    @FXML private TableColumn<UserDTO, String> colUsername;
    @FXML private TableColumn<UserDTO, String> colRole;
    @FXML private TableColumn<UserDTO, Double> colBalance;
    @FXML private TableColumn<UserDTO, String> colStatus;
    @FXML public static AdminUserManagementController instance;
    private final ObservableList<UserDTO> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        // Ánh xạ các cột tương ứng với các trường dữ liệu định nghĩa trong lớp UserDTO
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tbvUsers.setItems(userList);

        // Tải toàn bộ danh sách người dùng khi mở màn hình
        loadAllUsers();
    }

    /**
     * Gửi yêu cầu lấy toàn bộ người dùng từ database lên hệ thống Server
     */
    private void loadAllUsers() {
        userList.clear();
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_USERS", null));
    }

    /**
     * Xử lý khi Admin bấm nút "🔍 Tìm kiếm"
     */
    @FXML
    public void onSearchClick() {
        String keyword = txtSearchUser.getText().trim();
        if (keyword.isEmpty()) {
            loadAllUsers(); // Nếu để trống thì load lại tất cả
            return;
        }

        userList.clear();
        // Gửi chuỗi từ khóa (Username) lên server lọc dữ liệu
        SocketClient.sendRequest(new AuctionRequest("SEARCH_USER", keyword));
    }

    /**
     * Xử lý khi Admin chọn 1 dòng và bấm "🔒 Khóa tài khoản"
     */
    @FXML
    public void onLockUserClick() {
        changeUserStatusAction("LOCK_USER", "Đã gửi yêu cầu khóa tài khoản người dùng!");
    }

    /**
     * Xử lý khi Admin chọn 1 dòng và bấm "🔓 Mở khóa"
     */
    @FXML
    public void onUnlockUserClick() {
        changeUserStatusAction("UNLOCK_USER", "Đã gửi yêu cầu kích hoạt lại tài khoản!");
    }

    /**
     * Hàm dùng chung để xử lý yêu cầu thay đổi trạng thái hoạt động của tài khoản
     */
    private void changeUserStatusAction(String requestAction, String successMessage) {
        UserDTO selectedUser = tbvUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            AlertHelper.showError("Vui lòng chọn một người dùng từ danh sách trước!");
            return;
        }

        try {
            // Gửi thông tin user cần thay đổi trạng thái lên server
            SocketClient.sendRequest(new AuctionRequest(requestAction, selectedUser));
            AlertHelper.showInfo(successMessage);

            // Đợi server phản hồi hoặc bạn có thể tự thay đổi trạng thái tạm thời dưới client:
            // selectedUser.setStatus(requestAction.equals("LOCK_USER") ? "LOCKED" : "ACTIVE");
            // tbvUsers.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Thực hiện thao tác thất bại!");
        }
    }

    /**
     * Được gọi bởi luồng nhận dữ liệu từ Server để cập nhật dữ liệu vào bảng
     */
    public void updateTableData(List<UserDTO> users) {
        if (users != null) {
            userList.setAll(users);
        }
    }
}