package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.node.support.PathApi;
import io.jaspercloud.sdwan.platform.WindowsPlatform2Launcher;
import io.jaspercloud.sdwan.support.OsxShell;
import io.jaspercloud.sdwan.support.WinShell;
import io.jaspercloud.sdwan.util.CheckAdmin;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SdWanNodeLauncher {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        //type: manage、tunnel
        options.addOption("t", "type", true, "type");
        //action: install、uninstall、run
        options.addOption("a", "action", true, "action");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        options.addOption("f", "foreground", false, "foreground");
        options.addOption("debug", "debug", false, "debug");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        try {
            Logger logger;
            if (cmd.hasOption("log")) {
                String logFile = cmd.getOptionValue("log");
                logger = new LoggerSystem().init(logFile);
            } else {
                if (cmd.hasOption("debug")) {
                    logger = new LoggerSystem().initUserDir(false);
                } else {
                    logger = new LoggerSystem().initUserDir(true);
                }
            }
            if (PlatformDependent.isWindows()) {
                if (!CheckAdmin.checkWindows()) {
                    String path = PathApi.getExecutableFromWin();
                    String execArgs = StringUtils.join(args, " ");
                    WinShell.ShellExecuteW(path, execArgs, null, WinShell.SW_SHOW);
                    return;
                }
                WindowsPlatform2Launcher.startup(cmd);
            } else if (PlatformDependent.isOsx()) {
                if (!CheckAdmin.checkOsx()) {
                    System.out.println("sdwan需要root用户运行，请输入密码");
                    String path = PathApi.getExecutableFromOSX();
                    OsxShell.executeWaitFor(path, args);
                    return;
                }
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            } else {
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static TunSdWanNode startTunSdWanNode(Logger logger) throws Exception {
        logger.info("startTunSdWanNode");
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        config.setAutoReconnect(true);
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        return tunSdWanNode;
    }
}
