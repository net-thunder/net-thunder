package io.jaspercloud.sdwan.service;

import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.support.WinServiceManager;
import io.jaspercloud.sdwan.support.WinSvcUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TunnelService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static final String Type = "tunnel";

    public static String getTunnelServiceArgs() throws Exception {
        String javaPath = WinSvcUtil.getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add(javaPath);
        argList.add("-jar");
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add(Type);
        argList.add("-n");
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        String name = config.getTunName();
        argList.add(name);
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        return args;
    }

    public static void run(CommandLine cmd) throws Exception {
        String logPath = cmd.getOptionValue("log");
        Logger logger = new LoggerSystem().init(logPath);
        logger.info("TunnelService run");
        new TunnelService().runService(cmd);
    }

    private void runService(CommandLine cmd) throws Exception {
        logger.info("runService");
        logger.info("startServiceCtrlDispatcher");
        String serviceName = cmd.getOptionValue("n");
        String configPath = cmd.getOptionValue("c");
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
