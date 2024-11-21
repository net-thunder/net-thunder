package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui2.UpdateVersionController;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class UpdateVersionLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //windows
        String downloadUrl = getParameters().getNamed().get("url");
        String md5Hex = getParameters().getNamed().get("md5");
        FXMLLoader loader = new FXMLLoader();
        loader.setClassLoader(JavaFxMiniLauncher.getClassLoader());
        Parent root = loader.load(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/update.fxml"));
        Image icon = new Image(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/favicon.ico"));
        primaryStage.getIcons().add(icon);
        UpdateVersionController controller = loader.getController();
        controller.initStage(primaryStage);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
        controller.download(downloadUrl, md5Hex);
        primaryStage.setTitle("net-thunder");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 400, 150));
        primaryStage.show();
    }
}
