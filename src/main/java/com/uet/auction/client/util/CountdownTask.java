package com.uet.auction.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class CountdownTask {
    private Timeline timeline;

    public void start(LocalDateTime endTime, Label label) {
        stop(); // Dừng timeline cũ nếu có
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), endTime);
            long seconds = duration.getSeconds();
            if (seconds <= 0) {
                label.setText("ĐÃ KẾT THÚC");
                stop();
            } else {
                long h = seconds / 3600;
                long m = (seconds % 3600) / 60;
                long s = seconds % 60;
                label.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}