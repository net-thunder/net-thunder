package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.tranport.DataTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransportWrapper {

    private BlockingQueue<byte[]> dataQueue = new LinkedBlockingQueue<>();
    private DataTransport transport;

    public void setTransport(DataTransport transport) {
        this.transport = transport;
    }

    public DataTransport getTransport() {
        return transport;
    }

    public void appendWaitData(byte[] bytes) {
        dataQueue.add(bytes);
    }

    public List<byte[]> getWaitDataList() {
        List<byte[]> list = new ArrayList<>();
        dataQueue.drainTo(list);
        return list;
    }
}
