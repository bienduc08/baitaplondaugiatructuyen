package com.uet.auction.client.controller;

// CHÚ Ý: Đã sửa lại toàn bộ thư viện import cho chuẩn JavaFX
import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public class ProfileEditController {

    // Biến dùng để xử lý nút "Quay lại"
    public static Runnable onBackAction;

    @FXML private TextField txtFullName, txtEmail, txtPhone, txtUsername;
    @FXML private PasswordField txtOldPassword, txtNewPassword;
    @FXML private ImageView imgAvatar;

    @FXML
    public void initialize() {
        // Nạp dữ liệu người dùng từ Session
        UserDTO user = SessionManager.getCurrentUser();

        if (user != null) {
            txtUsername.setText(user.getUsername());
            txtFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            txtEmail.setText(user.getGmail() != null ? user.getGmail() : "");

            // Đã thêm lấy số điện thoại ở đây
            txtPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

            // Bo tròn ảnh đại diện (Do bên file FXML kích thước ảnh là 100x100 nên tâm là 50,50 và bán kính là 50)
            Circle clip = new Circle(50, 50, 50);
            imgAvatar.setClip(clip);
        }
    }

    @FXML
    public void onSave() {
        String fullName = txtFullName.getText().trim();
        String phone = txtPhone.getText().trim();
        String oldPass = txtOldPassword.getText(); // Không trim mật khẩu vì khoảng trắng cũng là ký tự
        String newPass = txtNewPassword.getText();

        // Kiểm tra số điện thoại nếu người dùng có nhập
        if (!phone.isEmpty() && !phone.matches("^0\\d{9}$")) {
            AlertHelper.showError("Số điện thoại không hợp lệ (Phải gồm 10 số và bắt đầu bằng 0)!");
            return;
        }

        // Kiểm tra logic đổi mật khẩu
        if ((!oldPass.isEmpty() && newPass.isEmpty()) || (oldPass.isEmpty() && !newPass.isEmpty())) {
            AlertHelper.showError("Để đổi mật khẩu, vui lòng nhập cả mật khẩu cũ và mới!");
            return;
        }

        // Đóng gói dữ liệu gửi lên Server
        String username = SessionManager.getCurrentUsername();
        Object[] updateData = new Object[]{username, fullName, phone, oldPass, newPass};

        // Gửi yêu cầu qua Socket
        SocketClient.sendRequest(new AuctionRequest("UPDATE_PROFILE", updateData));

        // Bạn có thể show thông báo tạm thời ở đây
        AlertHelper.showInfo("Đã gửi yêu cầu cập nhật thông tin!");
    }

    @FXML
    public void onCancel() {
        // Kích hoạt hàm quay lại trang trước
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    @FXML
    public void onChangeAvatar() {
        // Tạm thời hiển thị thông báo, bạn có thể tích hợp FileChooser vào đây sau
        AlertHelper.showInfo("Tính năng đổi ảnh đại diện đang được phát triển!");
    }
}