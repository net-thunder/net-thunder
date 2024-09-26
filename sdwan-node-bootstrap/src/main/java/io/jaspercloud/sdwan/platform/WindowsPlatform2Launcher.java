package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui2.MainWindow2;
import javafx.application.Application;
import javafx.application.Platform;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsPlatform2Launcher {

    private static final Logger logger = LoggerFactory.getLogger(WindowsPlatform2Launcher.class);

    public static void startup(CommandLine cmd) {
        try {
            Platform.setImplicitExit(false);
            new WindowsPlatform2Launcher().run(cmd);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void run(CommandLine cmd) throws Exception {
        Application.launch(MainWindow2.class, new String[0]);
    }
}
