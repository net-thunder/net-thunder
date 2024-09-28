package io.jaspercloud.sdwan.platform.ui2;

import io.jaspercloud.sdwan.node.ConfigSystem;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingController implements EventHandler<ActionEvent> {

    @FXML
    private TextArea settingText;

    @FXML
    private Button saveBtn;

    private Stage primaryStage;

    public static void showAndWait(Stage primaryStage) throws Exception {
        ClassLoader classLoader = SettingController.class.getClassLoader();
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(classLoader.getResource("ui/setting.fxml"));
        Parent root = loader.load();
        SettingController controller = loader.getController();
        controller.initStage(stage);
        Image icon = new Image(classLoader.getResourceAsStream("ui/favicon.ico"));
        stage.getIcons().add(icon);
        stage.setTitle("配置");
        stage.setResizable(false);
        stage.setScene(new Scene(root, 400, 250));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(primaryStage);
        stage.showAndWait();
    }

    @FXML
    public void initialize() throws Exception {
        saveBtn.setOnAction(this);
        String configText = ConfigSystem.getConfigText();
        settingText.setText(configText);
    }

    public void initStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void handle(ActionEvent event) {
        Control target = (Control) event.getTarget();
        if (target == saveBtn) {
            String configText = settingText.getText();
            ConfigSystem.saveConfig(configText);
            primaryStage.close();
        }
    }
}
