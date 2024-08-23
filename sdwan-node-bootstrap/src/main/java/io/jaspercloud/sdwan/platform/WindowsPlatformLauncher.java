package io.jaspercloud.sdwan.platform;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.support.WinServiceManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class WindowsPlatformLauncher {

    public static void startup(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("t", "type", true, "type");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        new WindowsPlatformLauncher().run(cmd);
    }

    private void run(CommandLine cmd) throws Exception {
        if (!cmd.hasOption("t")) {
            mainProcess(cmd);
        } else if ("start".equals(cmd.getOptionValue("t"))) {
            startService(cmd);
        } else if ("stop".equals(cmd.getOptionValue("t"))) {
            stopService(cmd);
        } else if ("uninstall".equals(cmd.getOptionValue("t"))) {
            uninstall(cmd);
        } else if ("service".equals(cmd.getOptionValue("t"))) {
            runService(cmd);
        }
    }

    private void mainProcess(CommandLine cmd) throws Exception {
        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }

    private void startService(CommandLine cmd) throws Exception {
        WinSvcUtil.startService(cmd);
    }

    private void stopService(CommandLine cmd) {
        WinSvcUtil.stopService(cmd);
    }

    private void uninstall(CommandLine cmd) {
        WinSvcUtil.uninstall(cmd);
    }

    private void runService(CommandLine cmd) throws Exception {
        String serviceName = cmd.getOptionValue("n");
        String logPath = cmd.getOptionValue("log");
        String configPath = cmd.getOptionValue("c");
        Logger logger = new LoggerSystem().init(logPath);
        logger.info("runService");
        logger.info("startServiceCtrlDispatcher");
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        WinServiceManager.startServiceCtrlDispatcher(serviceName, new WinServiceManager.ServiceProcHandler() {

            private TunSdWanNode tunSdWanNode;

            @Override
            public void start() throws Exception {
                logger.info("start service process");
                tunSdWanNode = new TunSdWanNode(config);
                tunSdWanNode.start();
                logger.info("service started");
            }

            @Override
            public void stop() throws Exception {
                logger.info("stop service process");
                tunSdWanNode.stop();
                logger.info("service stopped");
            }
        });
        logger.info("stopServiceCtrlDispatcher");
        System.exit(0);
    }
}
