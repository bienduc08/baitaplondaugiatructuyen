package com.uet.auction.controller;

import com.uet.auction.model.Item;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import com.uet.auction.service.AuctionService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AdminView implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminView.class);

    @FXML private Button          btnApprove;
    @FXML private TableView<Item> tablePending;
    @FXML private TextField       txtReason;
    @FXML private DatePicker      datePickerStart;
    @FXML private DatePicker      datePickerEnd;

    private User           user;
    private DataStorage    dataStorage;
    private AuctionService auctionService;

    public void initData(User user, DataStorage dataStorage, AuctionService auctionService) {
        if (!user.canManageSystem()) {
            log.warn("User [{}] không có quyền Admin", user.getUsername());
            new Alert(Alert.AlertType.ERROR, "Bạn không có quyền truy cập trang Admin!").show();
            return;
        }
        this.user           = user;
        this.dataStorage    = dataStorage;
        this.auctionService = auctionService;
        tablePending.getItems().setAll(dataStorage.getPendingItems());
        log.info("AdminView initData: user={}", user.getUsername());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("AdminView khởi tạo");
    }

    private void processRequest(String action) {
        try {
            Item selected = tablePending.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng chọn một sản phẩm!").showAndWait();
                return;
            }
            switch (action) {
                case "ADMIN_APPROVE"  -> handleApprove(selected);
                case "ADMIN_REJECT"   -> handleReject(selected);
                case "ADMIN_SET_TIME" -> handleSetTime(selected);
                default               -> log.warn("Action Admin không hợp lệ: {}", action);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý action Admin [{}]: {}", action, e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).show();
        }
    }

    private void handleApprove(Item item) {
        log.info("Duyệt sản phẩm: name={}", item.getName());
        auctionService.processApproval(item, true, "");
        tablePending.getItems().remove(item);
        new Alert(Alert.AlertType.INFORMATION, "Phê duyệt thành công: " + item.getName()).show();
    }

    private void handleReject(Item item) {
        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng nhập lý do từ chối!").showAndWait();
            return;
        }
        log.info("Từ chối sản phẩm: name={}, reason={}", item.getName(), reason);
        auctionService.processApproval(item, false, reason);
        tablePending.getItems().remove(item);
        txtReason.clear();
    }

    private void handleSetTime(Item item) {
        if (datePickerStart.getValue() == null || datePickerEnd.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đủ ngày bắt đầu và kết thúc!").showAndWait();
            return;
        }
        LocalDateTime start = datePickerStart.getValue().atStartOfDay();
        LocalDateTime end   = datePickerEnd.getValue().atTime(23, 59);
        if (!end.isAfter(start)) {
            new Alert(Alert.AlertType.WARNING, "Ngày kết thúc phải sau ngày bắt đầu!").showAndWait();
            return;
        }
        log.info("Cập nhật thời gian: itemId={}, start={}, end={}", item.getId(), start, end);
        auctionService.updateAuctionTime(item.getId(), start, end);
        new Alert(Alert.AlertType.INFORMATION, "Đã cập nhật thời gian đấu giá!").show();
    }

    @FXML public void onApproveButtonClick()  { processRequest("ADMIN_APPROVE"); }
    @FXML public void onRejectButtonClick()   { processRequest("ADMIN_REJECT"); }
    @FXML public void onSaveTimeButtonClick() { processRequest("ADMIN_SET_TIME"); }
}