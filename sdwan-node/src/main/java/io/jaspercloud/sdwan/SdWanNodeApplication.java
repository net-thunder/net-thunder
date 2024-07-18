package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.setting.yaml.YamlUtil;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/12
 */
public class SdWanNodeApplication {

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);
        SdWanNodeConfig config = loadConfig();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    private static SdWanNodeConfig loadConfig() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml")) {
            return YamlUtil.load(in, SdWanNodeConfig.class);
        }
    }
}
