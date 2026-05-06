package com.uet.auction.controller;

import com.uet.auction.model.ItemCategory;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class SellerView implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SellerView.class);

    @FXML private Button                 btnPost;
    @FXML private TextField              txtName;
    @FXML private TextField              txtDescription;
    @FXML private TextField              txtPrice;
    @FXML private ComboBox<ItemCategory> cbCategory;
    @FXML private Label                  lblWelcome;

    private User           user;
    private DataStorage    dataStorage;
    private AuctionService auctionService;

    public void initData(User user, DataStorage dataStorage, AuctionService auctionService) {
        if (!user.canSell()) {
            log.warn("User [{}] không có quyền Seller", user.getUsername());
            new Alert(Alert.AlertType.ERROR, "Bạn không có quyền đăng sản phẩm!").show();
            return;
        }
        this.user           = user;
        this.dataStorage    = dataStorage;
        this.auctionService = auctionService;
        lblWelcome.setText("Xin chào, " + user.getUsername());
        log.info("SellerView initData: user={}", user.getUsername());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategory.getItems().setAll(ItemCategory.values());
        cbCategory.setValue(ItemCategory.OTHER);
        log.info("SellerView khởi tạo");
    }

    private void processRequest(String action) {
        try {
            switch (action) {
                case "SELLER_POST"  -> handlePost();
                case "SELLER_CLEAR" -> handleClear();
                default             -> log.warn("Action Seller không hợp lệ: {}", action);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý action Seller [{}]: {}", action, e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).show();
        }
    }

    private void handlePost() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Tên sản phẩm không được để trống!").showAndWait();
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Giá phải là số hợp lệ!").showAndWait();
            return;
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            new Alert(Alert.AlertType.WARNING, "Giá khởi điểm phải lớn hơn 0!").showAndWait();
            return;
        }
        String description = txtDescription.getText().trim();
        ItemCategory category = cbCategory.getValue();

        log.info("Đăng sản phẩm: name={}, price={}, category={}, seller={}",
                name, price, category, user.getUsername());

        auctionService.submitItemForApproval(name, description, price,
                String.valueOf(user.getId()), category);

        new Alert(Alert.AlertType.INFORMATION, "Đã gửi sản phẩm chờ Admin duyệt!").show();
        handleClear();
        log.info("Đăng sản phẩm thành công: name={}", name);
    }

    private void handleClear() {
        txtName.clear();
        txtDescription.clear();
        txtPrice.clear();
        cbCategory.setValue(ItemCategory.OTHER);
    }

    @FXML public void onPostButtonClick()  { processRequest("SELLER_POST"); }
    @FXML public void onClearButtonClick() { processRequest("SELLER_CLEAR"); }
}