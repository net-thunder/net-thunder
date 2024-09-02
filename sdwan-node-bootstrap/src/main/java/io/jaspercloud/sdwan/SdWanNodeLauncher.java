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
        options.addOption("debug", "debug", false, "debug");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("debug")) {
            startTunSdWanNode();
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } else if (PlatformDependent.isWindows()) {
            WindowsPlatformLauncher.startup(cmd);
        } else {
            startTunSdWanNode();
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        }
    }

    private static TunSdWanNode startTunSdWanNode() throws Exception {
        Logger logger = new LoggerSystem().initUserDir();
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        return tunSdWanNode;
    }
}
