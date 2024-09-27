package io.jaspercloud.sdwan.platform.ui2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class MainWindow2 extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //windows
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ui/main2.fxml"));
        Parent root = loader.load();
        Image icon = new Image(getClass().getClassLoader().getResourceAsStream("ui/favicon.ico"));
        primaryStage.getIcons().add(icon);
        MainWindow2Controller controller = loader.getController();
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
            BufferedImage trayImage = ImageIO.read(getClass().getClassLoader().getResourceAsStream("ui/favicon.ico"));
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
