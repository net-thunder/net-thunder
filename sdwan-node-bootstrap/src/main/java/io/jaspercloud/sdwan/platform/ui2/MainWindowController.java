package io.jaspercloud.sdwan.platform.ui2;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.EventListener;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.support.HttpApi;
import io.jaspercloud.sdwan.support.OsxShell;
import io.jaspercloud.sdwan.util.*;
import io.netty.util.internal.PlatformDependent;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Collectors;

@Slf4j
public class MainWindowController implements EventHandler<ActionEvent> {

    @FXML
    private Label statusLab;

    @FXML
    private TextField vipText;

    @FXML
    private TextField macText;

    @FXML
    private ChoiceBox<NetworkInterfaceInfo> netSelect;

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
    private TunSdWanNode tunSdWanNode;

    @FXML
    public void initialize() throws Exception {
        queue = new SynchronousQueue<>();
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        stopBtn.setDisable(true);
        refreshBtn.setOnAction(this);
        settingBtn.setOnAction(this);
        netSelect.setConverter(new StringConverter<NetworkInterfaceInfo>() {
            @Override
            public String toString(NetworkInterfaceInfo interfaceInfo) {
                if (null == interfaceInfo) {
                    return null;
                }
                return interfaceInfo.getIp();
            }

            @Override
            public NetworkInterfaceInfo fromString(String s) {
                return null;
            }
        });
        netSelect.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<NetworkInterfaceInfo>() {
            @Override
            public void changed(ObservableValue<? extends NetworkInterfaceInfo> observableValue, NetworkInterfaceInfo oldValue, NetworkInterfaceInfo newValue) {
                NetworkInterfaceInfo interfaceInfo = observableValue.getValue();
                if (null != interfaceInfo) {
                    localIp = interfaceInfo.getIp();
                    macText.setText(interfaceInfo.getHardwareAddress());
                } else {
                    localIp = null;
                    macText.setText("-");
                }
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
            List<NetworkInterfaceInfo> netList = NetworkInterfaceUtil.findIpv4NetworkInterfaceInfo(true)
                    .stream()
                    .filter(e -> StringUtils.isNotEmpty(e.getHardwareAddress()))
                    .collect(Collectors.toList());
            netList = testNetList(netList);
            netSelect.getItems().clear();
            netSelect.getItems().addAll(netList);
            if (!netList.isEmpty()) {
                netSelect.getSelectionModel().select(0);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<NetworkInterfaceInfo> testNetList(List<NetworkInterfaceInfo> netList) throws Exception {
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        String httpServer = config.getHttpServer();
        InetSocketAddress httpAddress = SocketAddressUtil.parse(httpServer);
        List<NetworkInterfaceInfo> collect = netList.parallelStream().filter(interfaceInfo -> {
            Socket socket = new Socket();
            try {
                socket.bind(new InetSocketAddress(interfaceInfo.getIp(), 0));
                socket.connect(httpAddress, 1000);
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }).collect(Collectors.toList());
        return collect;
    }

    private void runService() throws Exception {
        while (true) {
            try {
                String action = queue.take();
                runAction(action);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void runAction(String action) throws Exception {
        if ("start".equals(action)) {
            SdWanNodeConfig config = new ConfigSystem().initUserDir();
            if (null == config) {
                Platform.runLater(() -> {
                    statusLab.setText("未配置接入信息");
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    netSelect.setDisable(false);
                    refreshBtn.setDisable(false);
                    settingBtn.setDisable(false);
                });
                return;
            }
            String controllerServer = HttpApi.getControllerServer(config);
            config.setControllerServer(controllerServer);
            config.setLocalAddress(localIp);
            tunSdWanNode = new TunSdWanNode(config);
            tunSdWanNode.addEventListener(new EventListener() {
                @Override
                public void onConnected() {
                    Platform.runLater(() -> {
                        statusLab.setText("已连接");
                        vipText.setText(tunSdWanNode.getLocalVip());
                        stopBtn.setDisable(false);
                    });
                }

                @Override
                public void onReConnecting() {
                    Platform.runLater(() -> {
                        statusLab.setText("重新连接中");
                    });
                }

                @Override
                public void onError(int code) {
                    if (SDWanProtos.MessageCode.NotGrant.getNumber() == code) {
                        stop(code);
                    } else if (SDWanProtos.MessageCode.Disabled.getNumber() == code) {
                        stop(code);
                    }
                }

                private void stop(int code) {
                    try {
                        tunSdWanNode.stop();
                        Platform.runLater(() -> {
                            if (SDWanProtos.MessageCode.NotGrant.getNumber() == code) {
                                statusLab.setText("客户端未授权");
                            } else if (SDWanProtos.MessageCode.Disabled.getNumber() == code) {
                                statusLab.setText("客户端被禁用");
                            }
                            startBtn.setDisable(false);
                            stopBtn.setDisable(true);
                            netSelect.setDisable(false);
                            refreshBtn.setDisable(false);
                            settingBtn.setDisable(false);
                        });
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

                @Override
                public void onErrorDisconnected() {
                    try {
                        tunSdWanNode.stop();
                        Platform.runLater(() -> {
                            statusLab.setText("连接异常");
                            startBtn.setDisable(false);
                            stopBtn.setDisable(true);
                            netSelect.setDisable(false);
                            refreshBtn.setDisable(false);
                            settingBtn.setDisable(false);
                        });
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
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
            } catch (ProcessCodeException e) {
                Platform.runLater(() -> {
                    if (SDWanProtos.MessageCode.NotGrant.getNumber() == e.getCode()) {
                        statusLab.setText("客户端未授权");
                    } else if (SDWanProtos.MessageCode.Disabled.getNumber() == e.getCode()) {
                        statusLab.setText("客户端被禁用");
                    } else {
                        statusLab.setText("连接异常: " + e.getCode());
                    }
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    netSelect.setDisable(false);
                    refreshBtn.setDisable(false);
                    settingBtn.setDisable(false);
                });
                throw e;
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
                stopBtn.setDisable(false);
            });
            tunSdWanNode.stop();
            Platform.runLater(() -> {
                statusLab.setText("已断开");
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
                netSelect.setDisable(false);
                refreshBtn.setDisable(false);
                settingBtn.setDisable(false);
                vipText.setText("-");
            });
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
                if (PlatformDependent.isOsx() && !CheckAdmin.checkOsx()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("提示");
                    alert.setHeaderText("需要以root权限启动");
                    alert.getButtonTypes().setAll(new ButtonType("是"));
                    alert.showAndWait();
                    String path = Jpackage.getAppPath();
                    OsxShell.executeScriptRoot(path, new String[0]);
                    System.exit(0);
                    return;
                }
                queue.offer("start");
            } else if (target == stopBtn) {
                queue.offer("stop");
            } else if (target == refreshBtn) {
                refreshNetList();
            } else if (target == settingBtn) {
                if (PlatformDependent.isOsx() && CheckAdmin.checkOsx()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("提示");
                    alert.setHeaderText("需要普通用户权限编辑");
                    alert.getButtonTypes().setAll(new ButtonType("是"));
                    alert.showAndWait();
                    return;
                }
                SettingWindowController.showAndWait(primaryStage);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }
}
