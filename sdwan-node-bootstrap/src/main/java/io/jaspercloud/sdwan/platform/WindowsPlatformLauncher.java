package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.platform.ui.MainWindow;
import io.jaspercloud.sdwan.service.ManagerService;
import io.jaspercloud.sdwan.service.TunnelService;
import javafx.application.Application;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class WindowsPlatformLauncher {

    public static void startup(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("t", "type", true, "type");
        options.addOption("a", "action", true, "action");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
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
