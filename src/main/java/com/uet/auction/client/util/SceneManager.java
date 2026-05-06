package com.uet.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;

public class SceneManager {
    public static void switchScene(Stage currentStage, String fxmlPath) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                currentStage.setScene(scene);
                currentStage.show();
            } catch (Exception e) {
                e.printStackTrace();
                AlertHelper.showError("Không thể tải màn hình: " + fxmlPath);
            }
        });
    }
}
