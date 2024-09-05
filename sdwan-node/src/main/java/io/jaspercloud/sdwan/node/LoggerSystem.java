package io.jaspercloud.sdwan.node;

import ch.qos.logback.classic.AsyncAppender;
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
        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file");
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);
        fileAppender.setFile(logFile);
        fileAppender.start();
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setName("async");
        asyncAppender.setContext(loggerContext);
        asyncAppender.addAppender(fileAppender);
        asyncAppender.start();
        logger.addAppender(asyncAppender);
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
        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file");
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);
        String logFile = new File(System.getProperty("user.dir"), "app.log").getAbsolutePath();
        fileAppender.setFile(logFile);
        fileAppender.start();
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setName("async");
        asyncAppender.setContext(loggerContext);
        asyncAppender.addAppender(fileAppender);
        asyncAppender.start();
        logger.addAppender(asyncAppender);
        return logger;
    }
}
