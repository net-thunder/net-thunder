package io.jaspercloud.sdwan.platform;

import ch.qos.logback.classic.Logger;
import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.support.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        } else if ("service".equals(cmd.getOptionValue("t"))) {
            runService(cmd);
        }
    }

    private void mainProcess(CommandLine cmd) throws Exception {
        String javaPath = getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add("-jar");
        String executeJarPath = getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add("start");
        argList.add("-n");
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        String name = config.getTunName();
        argList.add(name);
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        System.out.println(String.format("ShellExecuteW %s %s", javaPath, args));
        WinShell.ShellExecuteW(javaPath, args, null, WinShell.SW_HIDE);
    }

    private void startService(CommandLine cmd) throws Exception {
        String logPath = cmd.getOptionValue("log");
        String serviceName = cmd.getOptionValue("n");
        Logger logger = new LoggerSystem().init(logPath);
        logger.info("startService: {}", serviceName);
        try (WinServiceManager scm = WinServiceManager.openExecute()) {
            WinServiceManager.WinService winService = scm.openService(serviceName);
            try {
                if (null == winService) {
                    createService(logger, cmd);
                    winService = scm.openService(serviceName);
                }
                int status = queryServiceStatus(serviceName);
                logger.info("service status={}", status);
                if (Winsvc.SERVICE_RUNNING == status) {
                    return;
                }
                winService.start();
            } finally {
                if (null != winService) {
                    winService.close();
                }
            }
        }
    }

    private int queryServiceStatus(String serviceName) {
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                return winService.status();
            }
        }
    }

    private void stopService(CommandLine cmd) throws Exception {
        String logPath = cmd.getOptionValue("log");
        String serviceName = cmd.getOptionValue("n");
        Logger logger = new LoggerSystem().init(logPath);
        logger.info("stopService");
        try (WinServiceManager scm = WinServiceManager.openExecute()) {
            WinServiceManager.WinService winService = scm.openService(serviceName);
            if (null == winService) {
                return;
            }
            try {
                winService.stop();
            } finally {
                if (null != winService) {
                    winService.close();
                }
            }
        }
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

    private String getJavaPath() throws Exception {
        String javaHome = System.getProperty("java.home");
        String java = new File(new File(javaHome, "bin"), "java").getAbsolutePath();
        return java;
    }

    private String getExecuteJarPath() throws Exception {
        String userDir = System.getProperty("user.dir");
        String path = new File(userDir, "sdwan-node-bootstrap.jar").getAbsolutePath();
        return path;
    }

    private void createService(Logger logger, CommandLine cmd) throws Exception {
        List<String> argList = new ArrayList<>();
        argList.add(getJavaPath());
        argList.add("-jar");
        argList.add(getExecuteJarPath());
        argList.add("-t");
        argList.add("service");
        argList.add("-n");
        String name = cmd.getOptionValue("n");
        argList.add(name);
        String configPath = cmd.getOptionValue("c");
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        String logPath = cmd.getOptionValue("log");
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String path = StringUtils.join(argList, " ");
        logger.info("createService: {}", path);
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            scm.createService(name, path);
        }
    }
}
