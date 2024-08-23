package io.jaspercloud.sdwan.platform.ui;

import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.platform.WinSvcUtil;
import io.jaspercloud.sdwan.support.WinShell;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    @FXML
    private Button uninstallBtn;

    private ScheduledExecutorService scheduled;
    private SdWanNodeConfig config;

    public void initialize() throws Exception {
        scheduled = Executors.newSingleThreadScheduledExecutor();
        startBtn.setOnAction(this);
        stopBtn.setOnAction(this);
        uninstallBtn.setOnAction(this);
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        scheduled.scheduleAtFixedRate(() -> {
            try {
                int status = WinSvcUtil.queryServiceStatus(config.getTunName());
                Platform.runLater(() -> {
                    if (Winsvc.SERVICE_STOPPED == status) {
                        uninstallBtn.setDisable(false);
                    } else {
                        uninstallBtn.setDisable(true);
                    }
                    switch (status) {
                        case -1: {
                            statusLab.setText("status: not install");
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
    }

    @Override
    public void handle(ActionEvent event) {
        try {
            Control target = (Control) event.getTarget();
            if (target == startBtn) {
                shellExecuteService("start");
            } else if (target == stopBtn) {
                shellExecuteService("stop");
            } else if (target == uninstallBtn) {
                shellExecuteService("uninstall");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void shellExecuteService(String action) throws Exception {
        String javaPath = WinSvcUtil.getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add("-jar");
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add(action);
        argList.add("-n");
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        String name = config.getTunName();
        argList.add(name);
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        System.out.println(String.format("ShellExecuteW %s %s", javaPath, args));
        WinShell.ShellExecuteW(javaPath, args, null, WinShell.SW_HIDE);
    }
}
