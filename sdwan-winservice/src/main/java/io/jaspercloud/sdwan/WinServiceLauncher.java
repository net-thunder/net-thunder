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

    private Logger logger;

    public static void main(String[] args) throws Exception {
        new WinServiceLauncher().run(args);
    }

    private void run(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        options.addOption("d", "damon", false, "damon");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        logger = new LoggerSystem().init(cmd.getOptionValue("log"));
        if (cmd.hasOption("d")) {
            logger.info("run damon");
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
            logger.info("exit WinServiceLauncher");
            System.exit(0);
        } else {
            logger.info("run launcher");
            String name = cmd.getOptionValue("n");
            try (WinServiceManager scm = WinServiceManager.openExecute()) {
                WinServiceManager.WinService winService = scm.openService(name);
                if (null == winService) {
                    createService(cmd);
                    winService = scm.openService(name);
                }
                winService.start();
                winService.close();
            }
        }
    }

    private void createService(CommandLine cmd) throws Exception {
        List<String> argList = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        String java = new File(new File(javaHome, "bin"), "java").getAbsolutePath();
        argList.add(java);
        argList.add("-jar");
        Class<?> jarLauncher = Class.forName("org.springframework.boot.loader.JarLauncher");
        ProtectionDomain protectionDomain = jarLauncher.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();
        String jarPath = location.toURI().getPath().replaceAll("^/", "");
        if (!jarPath.endsWith(".jar")) {
            throw new IllegalArgumentException("jar path error");
        }
        argList.add(new File(jarPath).getAbsolutePath());
        argList.add("-n");
        String name = cmd.getOptionValue("n");
        argList.add(name);
        String configPath = cmd.getOptionValue("c");
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        String logPath = cmd.getOptionValue("log");
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        argList.add("-d");
        String path = StringUtils.join(argList, " ");
        logger.info("createService: {}", path);
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            scm.createService(name, path);
        }
    }
}
