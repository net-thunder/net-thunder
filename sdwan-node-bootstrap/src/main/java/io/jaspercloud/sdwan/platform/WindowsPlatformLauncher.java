package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui.MainWindow;
import io.jaspercloud.sdwan.service.ManagerService;
import io.jaspercloud.sdwan.service.TunnelService;
import javafx.application.Application;
import org.apache.commons.cli.CommandLine;

public class WindowsPlatformLauncher {

    public static void startup(CommandLine cmd) throws Exception {
        new WindowsPlatformLauncher().run(cmd);
    }

    private void run(CommandLine cmd) throws Exception {
        if (!cmd.hasOption("t")) {
            Application.launch(MainWindow.class, new String[0]);
        } else if (ManagerService.Type.equals(cmd.getOptionValue("t"))) {
            ManagerService.run(cmd);
        } else if (TunnelService.Type.equals(cmd.getOptionValue("t"))) {
            TunnelService.run(cmd);
        }
    }
}
