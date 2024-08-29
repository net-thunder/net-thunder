package io.jaspercloud.sdwan.platform.ui;

import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.platform.rpc.RpcInvoker;
import io.jaspercloud.sdwan.platform.rpc.WinSvcRpc;
import io.jaspercloud.sdwan.service.ManagerService;
import io.jaspercloud.sdwan.service.TunnelService;
import io.jaspercloud.sdwan.support.WinSvcUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainWindowController implements EventHandler<ActionEvent> {

    @FXML
    private Label statusLab;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    private ExecutorService executor;
    private ScheduledExecutorService scheduled;
    private SdWanNodeConfig config;
    private WinSvcRpc winSvcRpc;

    public void initialize() throws Exception {
        ManagerService.executeUnInstall();
        ManagerService.executeInstall();
        executor = Executors.newSingleThreadExecutor();
        scheduled = Executors.newSingleThreadScheduledExecutor();
        winSvcRpc = RpcInvoker.buildClient(WinSvcRpc.class);
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        config = new ConfigSystem().init(configPath);
        scheduled.scheduleAtFixedRate(() -> {
            try {
                int status = WinSvcUtil.queryServiceStatus(config.getTunName());
                Platform.runLater(() -> {
                    switch (status) {
                        case -1: {
                            statusLab.setText("status: stopped");
                            break;
                        }
                        case Winsvc.SERVICE_RUNNING: {
                            statusLab.setText("status: running");
                            break;
                        }
                        case Winsvc.SERVICE_START:
                        case Winsvc.SERVICE_START_PENDING: {
                            statusLab.setText("status: starting");
                            break;
                        }
                        case Winsvc.SERVICE_STOP:
                        case Winsvc.SERVICE_STOP_PENDING: {
                            statusLab.setText("status: stopping");
                            break;
                        }
                        case Winsvc.SERVICE_STOPPED: {
                            statusLab.setText("status: stopped");
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void handleWindowClose(WindowEvent event) {
        scheduled.shutdown();
        ManagerService.executeUnInstall();
        System.exit(0);
    }

    @Override
    public void handle(ActionEvent event) {
        executor.execute(() -> {
            try {
                Control target = (Control) event.getTarget();
                String serviceName = config.getTunName();
                int status = winSvcRpc.queryServiceStatus(serviceName);
                if (target == startBtn) {
                    if (Winsvc.SERVICE_RUNNING == status) {
                        winSvcRpc.stopService(serviceName);
                        winSvcRpc.deleteService(serviceName);
                    } else if (Winsvc.SERVICE_STOPPED == status) {
                        winSvcRpc.deleteService(serviceName);
                    }
                    String path = TunnelService.getTunnelServiceArgs();
                    winSvcRpc.createService(serviceName, path);
                    winSvcRpc.startService(serviceName);
                } else if (target == stopBtn) {
                    if (Winsvc.SERVICE_RUNNING == status) {
                        winSvcRpc.stopService(serviceName);
                    }
                    winSvcRpc.deleteService(serviceName);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }
}
