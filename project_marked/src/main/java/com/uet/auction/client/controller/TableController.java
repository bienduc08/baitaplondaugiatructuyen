package com.uet.auction.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/**
 * Base controller dùng chung cho các màn hình có TableView.
 * Kế thừa class này nếu muốn dùng lại logic load/refresh bảng.
 */
public abstract class TableController<T> {

    protected ObservableList<T> tableData = FXCollections.observableArrayList();

    /**
     * Setup cột cho TableView — subclass override để cấu hình riêng
     */
    protected abstract void setupTable();

    /**
     * Gửi request lấy dữ liệu từ server
     */
    protected abstract void loadData();

    /**
     * Cập nhật dữ liệu vào bảng (gọi từ ResponseListener)
     */
    public void updateTable(java.util.List<T> items) {
        javafx.application.Platform.runLater(() -> tableData.setAll(items));
    }
}