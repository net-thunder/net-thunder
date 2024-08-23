package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;

import java.util.concurrent.CountDownLatch;

public class SdWanNodeDebug {

    public static void main(String[] args) throws Exception {
        Logger logger = new LoggerSystem().initUserDir();
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
