package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.support.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class WinServiceLauncher {

    public static void main(String[] args) throws Exception {
        new WinServiceLauncher().run(args);
    }

    private void run(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("t", "type", true, "type");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
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
        Logger logger = new LoggerSystem().init(cmd.getOptionValue("log"));
        logger.info("startService");
        String name = cmd.getOptionValue("n");
        try (WinServiceManager scm = WinServiceManager.openExecute()) {
            WinServiceManager.WinService winService = scm.openService(name);
            if (null == winService) {
                createService(logger, cmd);
                winService = scm.openService(name);
            }
            winService.start();
            winService.close();
        }
    }

    private void stopService(CommandLine cmd) throws Exception {
        Logger logger = new LoggerSystem().init(cmd.getOptionValue("log"));
        logger.info("stopService");
    }

    private void runService(CommandLine cmd) throws Exception {
        Logger logger = new LoggerSystem().init(cmd.getOptionValue("log"));
        logger.info("runService");
        boolean dispatcher = WinServiceManager.startServiceCtrlDispatcher(cmd, new WinServiceManager.ServiceProcHandler() {

            private TunSdWanNode tunSdWanNode;

            @Override
            public void start() throws Exception {
                logger.info("start service proc");
                SdWanNodeConfig config = new ConfigSystem().init(cmd.getOptionValue("c"));
                tunSdWanNode = new TunSdWanNode(config);
                tunSdWanNode.start();
            }

            @Override
            public void stop() throws Exception {
                logger.info("stop service proc");
                tunSdWanNode.stop();
            }
        });
        logger.info("startServiceCtrlDispatcher result: {}", dispatcher);
        System.exit(0);
    }

    private String getJavaPath() throws Exception {
        String javaHome = System.getProperty("java.home");
        String java = new File(new File(javaHome, "bin"), "java").getAbsolutePath();
        return java;
    }

    private String getExecuteJarPath() throws Exception {
        Class<?> jarLauncher = Class.forName("org.springframework.boot.loader.JarLauncher");
        ProtectionDomain protectionDomain = jarLauncher.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();
        String jarPath = location.toURI().getPath().replaceAll("^/", "");
        if (!jarPath.endsWith(".jar")) {
            throw new IllegalArgumentException("jar path error");
        }
        String path = new File(jarPath).getAbsolutePath();
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
