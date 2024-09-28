package io.jaspercloud.sdwan.platform.ui2;

import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Collectors;

@Slf4j
public class MainWindow2Controller implements EventHandler<ActionEvent> {

    @FXML
    private Label statusLab;

    @FXML
    private Label vipLab;

    @FXML
    private ChoiceBox<String> netSelect;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Button settingBtn;

    private Stage primaryStage;
    private SynchronousQueue<String> queue;
    private String localIp;

    @FXML
    public void initialize() throws Exception {
        queue = new SynchronousQueue<>();
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        stopBtn.setDisable(true);
        settingBtn.setOnAction(this);
        netSelect.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                localIp = observableValue.getValue();
            }
        });
        refreshNetList();
        new Thread(() -> {
            try {
                runService();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    public void initStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void refreshNetList() {
        try {
            List<String> netList = NetworkInterfaceUtil.findIpv4NetworkInterfaceInfo(true)
                    .stream()
                    .filter(e -> StringUtils.isNotEmpty(e.getHardwareAddress()))
                    .map(e -> e.getIp())
                    .collect(Collectors.toList());
            netSelect.getItems().addAll(netList);
            if (!netList.isEmpty()) {
                netSelect.getSelectionModel().select(0);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private void runService() throws Exception {
        TunSdWanNode tunSdWanNode = new TunSdWanNode(new ConfigSystem().initUserDir()) {
            @Override
            protected void onErrorDisconnected() throws Exception {
                stop();
                Platform.runLater(() -> {
                    statusLab.setText("连接异常");
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    netSelect.setDisable(false);
                    refreshBtn.setDisable(false);
                    settingBtn.setDisable(false);
                });
            }
        };
        while (true) {
            try {
                String action = queue.take();
                if ("start".equals(action)) {
                    SdWanNodeConfig config = new ConfigSystem().initUserDir();
                    config.setLocalAddress(localIp);
                    tunSdWanNode.setConfig(config);
                    Platform.runLater(() -> {
                        statusLab.setText("连接中");
                        startBtn.setDisable(true);
                        stopBtn.setDisable(true);
                        netSelect.setDisable(true);
                        refreshBtn.setDisable(true);
                        settingBtn.setDisable(true);
                    });
                    try {
                        tunSdWanNode.start();
                        Platform.runLater(() -> {
                            statusLab.setText("已连接");
                            stopBtn.setDisable(false);
                            vipLab.setText(tunSdWanNode.getLocalVip());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            statusLab.setText("连接异常");
                            startBtn.setDisable(false);
                            stopBtn.setDisable(true);
                            netSelect.setDisable(false);
                            refreshBtn.setDisable(false);
                            settingBtn.setDisable(false);
                        });
                        throw e;
                    }
                } else if ("stop".equals(action)) {
                    Platform.runLater(() -> {
                        statusLab.setText("断开中");
                    });
                    tunSdWanNode.stop();
                    Platform.runLater(() -> {
                        statusLab.setText("已断开");
                        startBtn.setDisable(false);
                        stopBtn.setDisable(true);
                        netSelect.setDisable(false);
                        refreshBtn.setDisable(false);
                        settingBtn.setDisable(false);
                        vipLab.setText("-");
                    });
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void handleWindowClose(Stage primaryStage, WindowEvent event) {
        event.consume();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("提示");
        dialog.setHeaderText("是否后台运行？");
        dialog.setContentText("是: 后台运行 否: 结束程序");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
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
        try {
            Control target = (Control) event.getTarget();
            if (target == startBtn) {
                queue.offer("start");
            } else if (target == stopBtn) {
                queue.offer("stop");
            } else if (target == refreshBtn) {
                refreshNetList();
            } else if (target == settingBtn) {
                SettingController.showAndWait(primaryStage);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
