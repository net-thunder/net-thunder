package io.jaspercloud.sdwan.platform.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ui/main.fxml"));
        Parent root = loader.load();
        MainWindowController controller = loader.getController();
        primaryStage.setOnCloseRequest(controller::handleWindowClose);
        primaryStage.setTitle("SD-WAN Node");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

}