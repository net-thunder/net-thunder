package io.jaspercloud.sdwan.service;

import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.platform.rpc.RpcInvoker;
import io.jaspercloud.sdwan.platform.rpc.WinSvcRpc;
import io.jaspercloud.sdwan.platform.rpc.WinSvcRpcImpl;
import io.jaspercloud.sdwan.support.WinServiceManager;
import io.jaspercloud.sdwan.support.WinShell;
import io.jaspercloud.sdwan.support.WinSvcUtil;
import io.netty.channel.Channel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ManagerService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static final String Type = "manage";
    private static final String ServiceName = "NetThunderManager";

    public static void executeInstall() {
        String javaPath = WinSvcUtil.getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add("-jar");
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add(Type);
        argList.add("-a");
        argList.add("install");
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        WinShell.ShellExecuteW(javaPath, args, null, WinShell.SW_HIDE);
    }

    public static void executeUnInstall() {
        String javaPath = WinSvcUtil.getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add("-jar");
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add(Type);
        argList.add("-a");
        argList.add("uninstall");
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        WinShell.ShellExecuteW(javaPath, args, null, WinShell.SW_HIDE);
    }

    public static void run(CommandLine cmd) throws Exception {
        String logPath = cmd.getOptionValue("log");
        Logger logger = new LoggerSystem().init(logPath);
        try {
            if ("install".equals(cmd.getOptionValue("a"))) {
                logger.info("ManagerService install");
                String path = WinSvcUtil.getManageServiceArgs("run", ServiceName, logPath);
                WinSvcUtil.createService(ServiceName, path);
                WinSvcUtil.startService(ServiceName);
            } else if ("uninstall".equals(cmd.getOptionValue("a"))) {
                logger.info("ManagerService uninstall");
                WinSvcUtil.stopService(ServiceName);
                WinSvcUtil.deleteService(ServiceName);
            } else if ("run".equals(cmd.getOptionValue("a"))) {
                logger.info("ManagerService run");
                new ManagerService().runService(cmd);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void runService(CommandLine cmd) throws Exception {
        logger.info("runService");
        logger.info("startServiceCtrlDispatcher");
        WinServiceManager.startServiceCtrlDispatcher(ServiceName, new WinServiceManager.ServiceProcHandler() {

            private Channel channel;

            @Override
            public void start() throws Exception {
                logger.info("start service process");
                channel = RpcInvoker.exportServer(WinSvcRpc.class, new WinSvcRpcImpl(), WinSvcRpc.PORT);
                logger.info("service started");
            }

            @Override
            public void stop() throws Exception {
                logger.info("stop service process");
                channel.close().sync();
                logger.info("service stopped");
            }
        });
        logger.info("stopServiceCtrlDispatcher");
        System.exit(0);
    }
}
