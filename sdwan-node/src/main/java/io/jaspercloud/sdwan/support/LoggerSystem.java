package io.jaspercloud.sdwan.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;
import java.nio.charset.Charset;

public class LoggerSystem {

    public Logger init(String logFile) {
        if (null == logFile) {
            throw new IllegalArgumentException("logFile is null");
        }
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
        appender.setFile(logFile);
        appender.start();
        logger.addAppender(appender);
        return logger;
    }

    public Logger initUserDir() {
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
        String logFile = new File(System.getProperty("user.dir"), "out.log").getAbsolutePath();
        appender.setFile(logFile);
        appender.start();
        logger.addAppender(appender);
        return logger;
    }
}
