package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.platform.WindowsPlatformLauncher;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SdWanNodeLauncher {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("t", "type", true, "type");
        options.addOption("a", "action", true, "action");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        options.addOption("f", "foreground", false, "foreground");
        options.addOption("trace", "trace", false, "trace");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        Logger logger;
        if (cmd.hasOption("log")) {
            String logFile = cmd.getOptionValue("log");
            logger = new LoggerSystem().init(logFile);
        } else {
            logger = new LoggerSystem().initUserDir();
        }
        try {
            if (cmd.hasOption("f")) {
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            } else if (PlatformDependent.isWindows()) {
                WindowsPlatformLauncher.startup(cmd);
            } else {
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static TunSdWanNode startTunSdWanNode(Logger logger) throws Exception {
        logger.info("startTunSdWanNode");
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        return tunSdWanNode;
    }
}
