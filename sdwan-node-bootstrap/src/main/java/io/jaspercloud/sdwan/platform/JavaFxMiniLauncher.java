package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui2.MainWindowController;
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
        Application.launch(JavaFxMiniLauncher.class, new String[0]);
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
