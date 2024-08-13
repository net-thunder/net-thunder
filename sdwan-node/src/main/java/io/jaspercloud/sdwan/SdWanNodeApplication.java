package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.support.ConfigSystem;
import io.jaspercloud.sdwan.support.LoggerSystem;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;

import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/12
 */
public class SdWanNodeApplication {

    public static void main(String[] args) throws Exception {
        Logger logger = new LoggerSystem().initUserDir();
        SdWanNodeConfig config = new ConfigSystem().initStaticResource();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
