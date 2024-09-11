package io.jaspercloud.sdwan.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalTime {

    private static Logger logger = LoggerFactory.getLogger(GlobalTime.class);

    private static ThreadLocal<GlobalTime> threadLocal = new InheritableThreadLocal<>();

    private long s;

    public static void setStart(long s) {
        GlobalTime globalTime = threadLocal.get();
        if (null == globalTime) {
            return;
        }
        globalTime.s = s;
    }

    public static GlobalTime create() {
        GlobalTime globalTime = new GlobalTime();
        threadLocal.set(globalTime);
        return globalTime;
    }

    public static void log(String msg) {
        GlobalTime globalTime = threadLocal.get();
        if (null == globalTime) {
            return;
        }
        long s = globalTime.s;
        long e = System.nanoTime();
        double time = 1.0 * (e - s) / 1000 / 1000;
        logger.info("time={}, msg={}", time, msg);
    }

    public static void remove() {
        threadLocal.remove();
    }
}
