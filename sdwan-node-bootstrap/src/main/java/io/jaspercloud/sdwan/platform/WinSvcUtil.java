package io.jaspercloud.sdwan.platform;

import ch.qos.logback.classic.Logger;
import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.support.WinServiceManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class WinSvcUtil {

    private WinSvcUtil() {

    }

    public static String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        String java = new File(new File(javaHome, "bin"), "java").getAbsolutePath();
        return java;
    }

    public static String getExecuteJarPath() {
        String userDir = System.getProperty("user.dir");
        String path = new File(userDir, "sdwan-node-bootstrap.jar").getAbsolutePath();
        return path;
    }

    public static void createService(Logger logger, CommandLine cmd) throws Exception {
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

    public static void startService(CommandLine cmd) throws Exception {
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

    public static void stopService(CommandLine cmd) {
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

    public static int queryServiceStatus(String serviceName) {
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                if (null == winService) {
                    return -1;
                }
                return winService.status();
            }
        }
    }

    public static void uninstall(CommandLine cmd) {
        String logPath = cmd.getOptionValue("log");
        String serviceName = cmd.getOptionValue("n");
        Logger logger = new LoggerSystem().init(logPath);
        logger.info("uninstall");
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            WinServiceManager.WinService winService = scm.openService(serviceName);
            if (null == winService) {
                return;
            }
            winService.deleteService();
        }
    }
}
