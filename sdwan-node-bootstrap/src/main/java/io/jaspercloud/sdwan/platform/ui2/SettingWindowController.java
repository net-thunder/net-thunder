package io.jaspercloud.sdwan.platform.ui2;

import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.platform.JavaFxMiniLauncher;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SettingWindowController implements EventHandler<ActionEvent> {

    @FXML
    private TextArea settingText;

    @FXML
    private Button saveBtn;

    private Stage primaryStage;

    public static void showAndWait(Stage primaryStage) throws Exception {
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        loader.setClassLoader(JavaFxMiniLauncher.getClassLoader());
        Parent root = loader.load(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/setting.fxml"));
        SettingWindowController controller = loader.getController();
        controller.initStage(stage);
        Image icon = new Image(JavaFxMiniLauncher.getClassLoader().getResourceAsStream("ui/favicon.ico"));
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
        log.info("handle");
        Control target = (Control) event.getTarget();
        log.info("test: {} {}", target.getId(), saveBtn.getId());
        if (target == saveBtn) {
            log.info("saveBtn");
            String configText = settingText.getText();
            ConfigSystem.saveConfig(configText);
            primaryStage.close();
        }
    }
}
