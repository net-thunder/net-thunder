package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.support.ConfigSystem;
import io.jaspercloud.sdwan.support.LoggerSystem;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/12
 */
public class SdWanNodeApplication {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        SdWanNodeConfig config = new ConfigSystem().init(cmd.getOptionValue("c"));
        Logger logger = new LoggerSystem().init(cmd.getOptionValue("log"));
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
