package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProfileSellerController {

    public static ProfileSellerController instance;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderEmail;
    @FXML private Label lblHeaderPhoneNumber;
    @FXML private Label lblTotalBids;
    public static Runnable onBackAction;
    @FXML private Button btnDeposit;

    @FXML private TableView<ProductDTO>            tblMyProducts;
    @FXML private TableColumn<ProductDTO, String>  colProdName;
    @FXML private TableColumn<ProductDTO, BigDecimal>  colProdPrice;
    @FXML private TableColumn<ProductDTO, String>  colProdStatus;
    @FXML private TableColumn<ProductDTO, LocalDateTime> colProdTimeStart;
    @FXML private TableColumn<ProductDTO, LocalDateTime> colProdTimeEnd;

    private final ObservableList<ProductDTO> productList = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        instance = this;

        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            // Tên hiển thị chính
            if (lblUsername != null) lblUsername.setText(user.getUsername());
            if (lblRole != null) lblRole.setText("🏪 Người bán");
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
            if (lblHeaderPhoneNumber != null) {
                String phone = (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty())
                        ? user.getPhoneNumber() : "Chưa có SĐT";
                lblHeaderPhoneNumber.setText("📞 " + phone);
            }

            // Tải dữ liệu từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
                SocketClient.sendRequest(new AuctionRequest("GET_MY_PRODUCTS", username));
            }
        }

        setupTable();
        if (tblMyProducts != null) tblMyProducts.setItems(productList);
    }

    private void setupTable() {
        if (colProdName != null)
            colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));

        if (colProdPrice != null) {
            colProdPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            colProdPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }

        if (colProdStatus != null) {
            colProdStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colProdStatus.setCellFactory(col -> new TableCell<>() {
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

        if (colProdTimeStart != null) {
            colProdTimeStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
            colProdTimeStart.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "—" : item.format(FMT));
                }
            });
        }

        if (colProdTimeEnd != null) {
            colProdTimeEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            colProdTimeEnd.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "—" : item.format(FMT));
                }
            });
        }
    }

    public void displayMyProducts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            productList.setAll(products != null ? products : List.of());
            if (lblTotalBids != null)
                lblTotalBids.setText(String.valueOf(productList.size()));
        });
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

    public void handleDepositSuccess(BigDecimal newBalance) {
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
    @FXML
    public void onEditProfileClick() {
        try {
            // Tải giao diện chỉnh sửa hồ sơ
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProfileEdit.fxml"));
            javafx.scene.Node editNode = loader.load();

            // Kiểm tra và trỏ trực tiếp vào đúng MainBorderPane của SellerController
            if (SellerController.instance != null && SellerController.instance.getMainBorderPane() != null) {
                javafx.scene.layout.BorderPane mainPane = SellerController.instance.getMainBorderPane();
                javafx.scene.Node previousCenterView = mainPane.getCenter();

                // Thiết lập nút Quay lại trả về đúng màn hình Profile cũ của Seller
                ProfileEditController.onBackAction = () -> {
                    mainPane.setCenter(previousCenterView);
                };

                // Đẩy giao diện chỉnh sửa vào giữa vùng hiển thị
                mainPane.setCenter(editNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở trang chỉnh sửa hồ sơ!");
        }
    }
    // Hàm để làm mới dữ liệu
    public void refreshProfileData() {
        UserDTO currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {

            // Cập nhật Tên hiển thị
            if (lblHeaderName != null) {
                String fullName = (currentUser.getFullName() != null && !currentUser.getFullName().isEmpty())
                        ? currentUser.getFullName() : currentUser.getUsername();
                lblHeaderName.setText("👤 " + fullName);
            }

            // Cập nhật Email
            if (lblHeaderEmail != null) {
                String gmail = (currentUser.getGmail() != null && !currentUser.getGmail().isEmpty())
                        ? currentUser.getGmail() : currentUser.getUsername() + "@gmail.com";
                lblHeaderEmail.setText("✉ " + gmail);
            }

            // Cập nhật Số điện thoại
            if (lblHeaderPhoneNumber != null) {
                String phone = (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty())
                        ? currentUser.getPhoneNumber() : "Chưa có SĐT";
                lblHeaderPhoneNumber.setText("📞 " + phone);
            }
        }
    }
}