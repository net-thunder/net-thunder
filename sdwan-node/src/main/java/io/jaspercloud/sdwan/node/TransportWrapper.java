package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.tranport.DataTransport;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransportWrapper {

    private BlockingQueue<DataPacket> dataQueue = new LinkedBlockingQueue<>();
    private volatile DataTransport transport;

    public void setTransport(DataTransport transport) {
        this.transport = transport;
    }

    public DataTransport getTransport() {
        return transport;
    }

    public void transfer(String vip, byte[] bytes) {
        if (null == transport) {
            //in the election
            dataQueue.add(new DataPacket(vip, bytes));
            return;
        }
        sendWaitData();
        transport.transfer(vip, bytes);
    }

    public void sendWaitData() {
        List<DataPacket> list = new ArrayList<>();
        dataQueue.drainTo(list);
        list.forEach(packet -> {
            transport.transfer(packet.getVip(), packet.getBytes());
        });
    }

    @Getter
    public static class DataPacket {

        private String vip;
        private byte[] bytes;

        public DataPacket(String vip, byte[] bytes) {
            this.vip = vip;
            this.bytes = bytes;
        }
    }
}
