package io.jaspercloud.sdwan.platform.ui2;

import cn.hutool.crypto.digest.DigestUtil;
import io.jaspercloud.sdwan.node.support.PathApi;
import io.jaspercloud.sdwan.support.OsxShell;
import io.jaspercloud.sdwan.support.WinShell;
import io.jaspercloud.sdwan.util.HttpClient;
import io.jaspercloud.sdwan.util.Jpackage;
import io.jaspercloud.sdwan.util.MessageBox;
import io.netty.util.internal.PlatformDependent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;

@Slf4j
public class UpdateVersionController implements EventHandler<ActionEvent> {

    @FXML
    private Label label;

    @FXML
    private ProgressBar progress;

    @FXML
    private Button btn;

    private Stage primaryStage;
    private boolean downloadFinish = false;

    private static final String ConfigFormat = "" +
            "[Application]\n" +
            "app.classpath=$APPDIR/%s\n" +
            "app.mainclass=org.springframework.boot.loader.launch.JarLauncher\n" +
            "            \n" +
            "[JavaOptions]\n" +
            "java-options=-Djpackage.app-version=1.0" +
            "";

    @FXML
    public void initialize() throws Exception {
        btn.setText("取消");
        btn.setOnAction(this);
    }

    public void initStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void handle(ActionEvent event) {
        try {
            Control target = (Control) event.getTarget();
            if (target == btn) {
                if (downloadFinish) {
                    if (PlatformDependent.isWindows()) {
                        String path = Jpackage.getAppPath();
                        WinShell.start(path, new String[0]);
                    } else if (PlatformDependent.isOsx()) {
                        String path = Jpackage.getAppPath();
                        OsxShell.executeScript(path, new String[0]);
                    }
                }
                System.exit(0);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void download(String downloadUrl, String md5Hex) throws Exception {
        new Thread(() -> {
            try {
                String name = String.format("sdwan-node-bootstrap-%s.jar", System.currentTimeMillis());
                File file = new File(PathApi.getAppDir(), name);
                HttpClient.download(downloadUrl, file, (e) -> {
                    Platform.runLater(() -> {
                        progress.setProgress(e);
                    });
                }, () -> {
                    String code = DigestUtil.md5Hex(file);
                    if (!StringUtils.equals(code, md5Hex)) {
                        MessageBox.showError("校验文件md5失败");
                        System.exit(0);
                        return;
                    }
                    updateCfg(name);
                    Platform.runLater(() -> {
                        label.setText("下载完成");
                        btn.setText("完成");
                    });
                    downloadFinish = true;
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                MessageBox.showError("更新下载请求失败");
            }
        }).start();
    }

    private void updateCfg(String name) {
        String config = String.format(ConfigFormat, name);
        try {
            File file = new File(PathApi.getAppDir(), "net-thunder.cfg");
            FileCopyUtils.copy(config.getBytes(), file);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
