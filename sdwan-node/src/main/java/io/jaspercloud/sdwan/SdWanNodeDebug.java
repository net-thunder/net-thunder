package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.TunSdWanNode;

import java.util.concurrent.CountDownLatch;

public class SdWanNodeDebug {

    public static void main(String[] args) throws Exception {
        Logger logger = new LoggerSystem().initUserDir(true);
        TunSdWanNode mainSdWanNode = new TunSdWanNode(new ConfigSystem().initUserDir());
        mainSdWanNode.start();
        logger.info("SdWanNodeDebug started");
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
