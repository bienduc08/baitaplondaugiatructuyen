package com.uet.auction.client.util;

import javafx.application.Platform;
import javafx.scene.control.Label;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

public class CountdownTask {
    private Timer timer;

    public void start(LocalDateTime endTime, Label label) {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Duration duration = Duration.between(LocalDateTime.now(), endTime);
                long seconds = duration.getSeconds();

                if (seconds <= 0) {
                    Platform.runLater(() -> label.setText("ĐÃ KẾT THÚC"));
                    timer.cancel();
                } else {
                    long h = seconds / 3600;
                    long m = (seconds % 3600) / 60;
                    long s = seconds % 60;
                    String timeStr = String.format("%02d:%02d:%02d", h, m, s);

                    // Platform.runLater là bắt buộc để cập nhật giao diện từ luồng phụ
                    Platform.runLater(() -> label.setText(timeStr));
                }
            }
        }, 0, 1000);
    }

    public void stop() {
        if (timer != null) timer.cancel();
    }
}
