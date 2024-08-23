package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.platform.WindowsPlatformLauncher;
import io.netty.util.internal.PlatformDependent;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SdWanNodeLauncher {

    public static void main(String[] args) throws Exception {
        if (PlatformDependent.isWindows()) {
            WindowsPlatformLauncher.startup(args);
        } else {
            Logger logger = new LoggerSystem().initUserDir();
            SdWanNodeConfig config = new ConfigSystem().initUserDir();
            TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
            tunSdWanNode.start();
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        }
    }
}
