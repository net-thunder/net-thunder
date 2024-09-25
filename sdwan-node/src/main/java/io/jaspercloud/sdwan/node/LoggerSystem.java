package io.jaspercloud.sdwan.node;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

public class LoggerSystem {

    public Logger init(String logFile) {
        if (null == logFile) {
            throw new IllegalArgumentException("logFile is null");
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        loggerContext.reset();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);
        //config
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        String charset;
        if (PlatformDependent.isWindows()) {
            charset = "gbk";
        } else {
            charset = "utf-8";
        }
        encoder.setCharset(Charset.forName(charset));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
        encoder.start();
        //consoleAppender
        ConsoleAppender consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        //fileAppender
        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file");
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);
        fileAppender.setFile(logFile);
        fileAppender.start();
        //asyncAppender
        boolean async = true;
        if (async) {
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setName("async-console");
            asyncAppender.setContext(loggerContext);
            asyncAppender.addAppender(consoleAppender);
            asyncAppender.start();
            logger.addAppender(asyncAppender);
        } else {
            logger.addAppender(consoleAppender);
        }
        if (async) {
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setName("async-file");
            asyncAppender.setContext(loggerContext);
            asyncAppender.addAppender(fileAppender);
            asyncAppender.start();
            logger.addAppender(asyncAppender);
        } else {
            logger.addAppender(fileAppender);
        }
        return logger;
    }

    public Logger initUserDir(boolean async) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        loggerContext.reset();
        Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.INFO);
        //config
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        String charset;
        if (PlatformDependent.isWindows()) {
            charset = "gbk";
        } else {
            charset = "utf-8";
        }
        encoder.setCharset(Charset.forName(charset));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
        encoder.start();
        //consoleAppender
        ConsoleAppender consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        //fileAppender
        String logFile = new File(System.getProperty("user.dir"), "app.log").getAbsolutePath();
        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file");
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);
        fileAppender.setFile(logFile);
        fileAppender.start();
        //asyncAppender
        if (async) {
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setName("async-console");
            asyncAppender.setContext(loggerContext);
            asyncAppender.addAppender(consoleAppender);
            asyncAppender.start();
            logger.addAppender(asyncAppender);
        } else {
            logger.addAppender(consoleAppender);
        }
        if (async) {
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setName("async-file");
            asyncAppender.setContext(loggerContext);
            asyncAppender.addAppender(fileAppender);
            asyncAppender.start();
            logger.addAppender(asyncAppender);
        } else {
            logger.addAppender(fileAppender);
        }
        return logger;
    }
}
