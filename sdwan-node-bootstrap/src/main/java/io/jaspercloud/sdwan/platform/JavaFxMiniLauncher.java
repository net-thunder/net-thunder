package io.jaspercloud.sdwan.platform;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.support.PathApi;
import io.jaspercloud.sdwan.platform.ui2.MainWindowController;
import io.jaspercloud.sdwan.support.HttpApi;
import io.jaspercloud.sdwan.util.AppFile;
import io.jaspercloud.sdwan.util.PlatformUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class JavaFxMiniLauncher extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxMiniLauncher.class);

    public static ClassLoader getClassLoader() {
        return JavaFxMiniLauncher.class.getClassLoader();
    }

    public static void startup(CommandLine cmd) {
        try {
            Platform.setImplicitExit(false);
            new JavaFxMiniLauncher().run(cmd);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void run(CommandLine cmd) throws Exception {
        logger.info("appDir: {}", PathApi.getAppDir());
        if (updateVersion()) {
            return;
        }
        Application.launch(JavaFxMiniLauncher.class, new String[0]);
    }

    private boolean updateVersion() throws Exception {
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        if (!config.getAutoUpdateVersion()) {
            return false;
        }
        File jarFile = AppFile.getLauncherJar();
        String md5Hex = DigestUtil.md5Hex(jarFile);
        String platform = PlatformUtil.normalizedOs();
        JSONObject result = HttpApi.checkVersion(config, platform, md5Hex);
        Integer code = result.getByPath("code", Integer.class);
        if (200 == code) {
            String path = result.getByPath("data.path", String.class);
            String md5 = result.getByPath("data.md5", String.class);
            String url = String.format("http://%s%s", config.getHttpServer(), path);
            Application.launch(UpdateVersionLauncher.class, new String[]{
                    String.format("--url=%s", url),
                    String.format("--md5=%s", md5)
            });
            return true;
        }
        return false;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //windows
        FXMLLoader loader = new FXMLLoader();
        loader.setClassLoader(JavaFxMiniLauncher.getClassLoader());
        Parent root = loader.load(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/main2.fxml"));
        Image icon = new Image(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/favicon.ico"));
        primaryStage.getIcons().add(icon);
        MainWindowController controller = loader.getController();
        controller.initStage(primaryStage);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                if (SystemTray.isSupported()) {
                    controller.handleWindowClose(primaryStage, windowEvent);
                } else {
                    System.exit(0);
                }
            }
        });
        primaryStage.setTitle("net-thunder");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 400, 350));
        primaryStage.show();
        //tray
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu menu = new PopupMenu();
            BufferedImage trayImage = ImageIO.read(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/favicon.ico"));
            TrayIcon trayIcon = new TrayIcon(trayImage, "net-thunder", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Platform.runLater(() -> {
                            primaryStage.show();
                        });
                    }
                }
            });
            tray.add(trayIcon);
        }
    }
}
