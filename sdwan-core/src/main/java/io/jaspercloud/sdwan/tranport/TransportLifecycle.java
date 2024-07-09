package io.jaspercloud.sdwan.tranport;

/**
 * @author jasper
 * @create 2024/7/5
 */
public interface TransportLifecycle {

    boolean isRunning();

    void start() throws Exception;

    void stop() throws Exception;
}
