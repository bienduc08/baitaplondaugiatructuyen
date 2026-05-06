package com.uet.auction.controller;

import com.uet.auction.model.User;
import com.uet.auction.model.UserRole;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginView implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    @FXML private TextField          txtUsername;
    @FXML private ComboBox<UserRole> cbRole;

    private DataStorage    dataStorage;
    private AuctionService auctionService;

    public LoginView() {}

    public LoginView(DataStorage dataStorage, AuctionService auctionService) {
        this.dataStorage    = dataStorage;
        this.auctionService = auctionService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbRole.setItems(FXCollections.observableArrayList(UserRole.values()));
        cbRole.setValue(UserRole.BIDDER);
        log.info("LoginView khởi tạo");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            String username = txtUsername.getText().trim();
            if (username.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng nhập tên người dùng!").showAndWait();
                return;
            }
            UserRole role = cbRole.getValue();
            log.info("Đăng nhập: username={}, role={}", username, role);

            String action = switch (role) {
                case ADMIN  -> "LOGIN_ADMIN";
                case SELLER -> "LOGIN_SELLER";
                case BIDDER -> "LOGIN_BIDDER";
                case USER   -> "LOGIN_USER";
            };
            processRequest(action, event, username, role);

        } catch (Exception e) {
            log.error("Lỗi đăng nhập: {}", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).show();
        }
    }

    private void processRequest(String action, ActionEvent event,
                                String username, UserRole role) throws IOException {
        switch (action) {
            case "LOGIN_ADMIN"          -> handleLoginAdmin(event, username, role);
            case "LOGIN_SELLER"         -> handleLoginSeller(event, username, role);
            case "LOGIN_BIDDER",
                 "LOGIN_USER"           -> handleLoginUser(event, username, role);
            default                     -> log.warn("Action login không hợp lệ: {}", action);
        }
    }

    private void handleLoginAdmin(ActionEvent event, String username, UserRole role)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/uet/auction/view/admin.fxml"));
        Parent root = loader.load();
        AdminView controller = loader.getController();
        controller.initData(buildUser(username, role), dataStorage, auctionService);
        switchScene(event, root, "Admin — " + username);
        log.info("Mở AdminView: user={}", username);
    }

    private void handleLoginSeller(ActionEvent event, String username, UserRole role)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/uet/auction/view/seller.fxml"));
        Parent root = loader.load();
        SellerView controller = loader.getController();
        controller.initData(buildUser(username, role), dataStorage, auctionService);
        switchScene(event, root, "Seller — " + username);
        log.info("Mở SellerView: user={}", username);
    }

    private void handleLoginUser(ActionEvent event, String username, UserRole role)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/uet/auction/view/user.fxml"));
        Parent root = loader.load();
        UserView controller = loader.getController();
        controller.initData(buildUser(username, role),
                new AuctionController(auctionService), dataStorage);
        switchScene(event, root, "User — " + username);
        log.info("Mở UserView: user={}", username);
    }

    private User buildUser(String username, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setRole(role);
        u.setBalance(BigDecimal.ZERO);
        return u;
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }
}