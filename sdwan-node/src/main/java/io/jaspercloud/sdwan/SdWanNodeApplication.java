package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import cn.hutool.setting.yaml.YamlUtil;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/12
 */
public class SdWanNodeApplication {

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        initLogger(null);
        SdWanNodeConfig config = loadConfig();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    private static void initLogger(String logFile) {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setCharset(Charset.forName("utf-8"));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
        encoder.start();

        FileAppender appender = new FileAppender();
        appender.setContext(loggerContext);
        appender.setName("file");
        appender.setEncoder(encoder);
        if (StringUtils.isEmpty(logFile)) {
            logFile = new File(System.getProperty("user.dir"), "out.log").getAbsolutePath();
        }
        appender.setFile(logFile);
        appender.start();
        logger.addAppender(appender);
    }

    private static SdWanNodeConfig loadConfig() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml")) {
            return YamlUtil.load(in, SdWanNodeConfig.class);
        }
    }
}
