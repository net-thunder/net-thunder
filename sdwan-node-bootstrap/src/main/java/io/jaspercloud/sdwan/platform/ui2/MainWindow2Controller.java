package io.jaspercloud.sdwan.platform.ui2;

import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.SynchronousQueue;

@Slf4j
public class MainWindow2Controller implements EventHandler<ActionEvent> {

    @FXML
    private Label statusLab;

    @FXML
    private Label vipLab;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    private SynchronousQueue<String> queue;

    @FXML
    public void initialize() throws Exception {
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        stopBtn.setDisable(true);
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        queue = new SynchronousQueue<>();
        new Thread(() -> {
            while (true) {
                try {
                    String action = queue.take();
                    if ("start".equals(action)) {
                        Platform.runLater(() -> {
                            statusLab.setText("连接中");
                        });
                        tunSdWanNode.start();
                        Platform.runLater(() -> {
                            statusLab.setText("已连接");
                            startBtn.setDisable(true);
                            stopBtn.setDisable(false);
                            vipLab.setText(tunSdWanNode.getLocalVip());
                        });
                    } else if ("stop".equals(action)) {
                        Platform.runLater(() -> {
                            statusLab.setText("断开中");
                        });
                        tunSdWanNode.stop();
                        Platform.runLater(() -> {
                            statusLab.setText("已断开");
                            startBtn.setDisable(false);
                            stopBtn.setDisable(true);
                            vipLab.setText("-");
                        });
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }).start();
    }

    public void handleWindowClose(Stage primaryStage, WindowEvent event) {
        event.consume();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("提示");
        dialog.setHeaderText("是否后台运行？");
        dialog.setContentText("是: 后台运行 否: 结束程序");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                primaryStage.hide();
            } else if (response == ButtonType.NO) {
                System.exit(0);
            }
        });
    }

    @Override
    public void handle(ActionEvent event) {
        Control target = (Control) event.getTarget();
        startBtn.setDisable(true);
        stopBtn.setDisable(true);
        if (target == startBtn) {
            queue.offer("start");
        } else if (target == stopBtn) {
            queue.offer("stop");
        }
    }
}
