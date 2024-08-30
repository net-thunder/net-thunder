package io.jaspercloud.sdwan.platform.ui;

import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.platform.rpc.RpcInvoker;
import io.jaspercloud.sdwan.platform.rpc.TunnelInfo;
import io.jaspercloud.sdwan.platform.rpc.TunnelRpc;
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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class MainWindowController implements EventHandler<ActionEvent> {

    @FXML
    private Label statusLab;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Label controllerServer;

    @FXML
    private Label stunServer;

    @FXML
    private Label relayServer;

    @FXML
    private Label vipCidr;

    @FXML
    private Label localVip;

    @FXML
    private Label sdwanLocalPort;

    @FXML
    private Label p2pLocalPort;

    @FXML
    private Label relayLocalPort;

    private ExecutorService executor;
    private ScheduledExecutorService scheduled;
    private SdWanNodeConfig config;
    private WinSvcRpc winSvcRpc;
    private TunnelRpc tunnelRpc;
    private boolean runningStatus;
    private ScheduledFuture scheduledFuture;
    private Lock lock = new ReentrantLock();

    @FXML
    public void initialize() throws Exception {
        ManagerService.executeUnInstall();
        ManagerService.executeInstall();
        executor = Executors.newSingleThreadExecutor();
        scheduled = Executors.newSingleThreadScheduledExecutor();
        winSvcRpc = RpcInvoker.buildClient(WinSvcRpc.class, WinSvcRpc.PORT);
        tunnelRpc = RpcInvoker.buildClient(TunnelRpc.class, TunnelRpc.PORT);
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        runningStatus = true;
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        config = new ConfigSystem().init(configPath);
        scheduledFuture = scheduledTask();
    }

    public void handleWindowClose(WindowEvent event) {
        runningStatus = false;
        scheduled.shutdown();
        ManagerService.executeUnInstall();
        System.exit(0);
    }

    @Override
    public void handle(ActionEvent event) {
        executor.execute(() -> {
            if (!runningStatus) {
                return;
            }
            lock.lock();
            try {
                Control target = (Control) event.getTarget();
                String serviceName = config.getTunName();
                int status = winSvcRpc.queryServiceStatus(serviceName);
                if (target == startBtn) {
                    String path = TunnelService.getTunnelServiceArgs();
                    if (Winsvc.SERVICE_RUNNING == status) {
                        return;
                    } else if (-1 == status) {
                        winSvcRpc.createService(serviceName, path);
                    }
                    winSvcRpc.startService(serviceName);
                } else if (target == stopBtn) {
                    if (Winsvc.SERVICE_RUNNING == status) {
                        winSvcRpc.stopService(serviceName);
                    }
                    winSvcRpc.deleteService(serviceName);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        });
    }

    private ScheduledFuture scheduledTask() {
        return scheduled.scheduleAtFixedRate(() -> {
            if (!runningStatus) {
                return;
            }
            if (!ping(WinSvcRpc.PORT)) {
                return;
            }
            try {
                int status = winSvcRpc.queryServiceStatus(config.getTunName());
                Platform.runLater(() -> {
                    switch (status) {
                        case -1: {
                            statusLab.setText("status: not started");
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
                if (Winsvc.SERVICE_RUNNING == status) {
                    TunnelInfo tunnelInfo = tunnelRpc.getTunnelInfo();
                    Platform.runLater(() -> {
                        controllerServer.setText(tunnelInfo.getControllerServer());
                        stunServer.setText(tunnelInfo.getStunServer());
                        relayServer.setText(tunnelInfo.getRelayServer());
                        vipCidr.setText(tunnelInfo.getVipCidr());
                        localVip.setText(tunnelInfo.getLocalVip());
                        sdwanLocalPort.setText(String.valueOf(tunnelInfo.getSdwanLocalPort()));
                        p2pLocalPort.setText(String.valueOf(tunnelInfo.getP2pLocalPort()));
                        relayLocalPort.setText(String.valueOf(tunnelInfo.getRelayLocalPort()));
                    });
                } else {
                    Platform.runLater(() -> {
                        controllerServer.setText("-");
                        stunServer.setText("-");
                        relayServer.setText("-");
                        vipCidr.setText("-");
                        localVip.setText("-");
                        sdwanLocalPort.setText("-");
                        p2pLocalPort.setText("-");
                        relayLocalPort.setText("-");
                    });
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
    }

    private boolean ping(int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", port), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
