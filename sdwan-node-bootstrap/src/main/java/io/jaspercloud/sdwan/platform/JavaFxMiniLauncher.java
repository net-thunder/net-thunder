package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui2.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFxMiniLauncher {

    private static final Logger logger = LoggerFactory.getLogger(JavaFxMiniLauncher.class);

    public static void startup(CommandLine cmd) {
        try {
            Platform.setImplicitExit(false);
            new JavaFxMiniLauncher().run(cmd);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void run(CommandLine cmd) throws Exception {
        Application.launch(MainWindow.class, new String[0]);
    }
}
