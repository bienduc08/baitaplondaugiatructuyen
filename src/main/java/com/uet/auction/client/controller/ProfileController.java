package com.uet.auction.client.controller;

import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;

    @FXML
    public void initialize() {
        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            usernameLabel.setText(user.getUsername());
            roleLabel.setText(user.getRole());
        } else {
            usernameLabel.setText("Chưa đăng nhập");
            roleLabel.setText("—");
        }
    }
}