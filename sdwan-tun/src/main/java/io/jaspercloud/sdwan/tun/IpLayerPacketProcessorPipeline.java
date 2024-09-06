package io.jaspercloud.sdwan.tun;

import java.util.ArrayList;
import java.util.List;

public class IpLayerPacketProcessorPipeline implements IpLayerPacketProcessor {

    private List<IpLayerPacketProcessor> list = new ArrayList<>();

    public void add(IpLayerPacketProcessor processor) {
        list.add(processor);
    }

    @Override
    public void input(IpLayerPacket packet) {
        for (IpLayerPacketProcessor processor : list) {
            processor.input(packet);
        }
    }

    @Override
    public void output(IpLayerPacket packet) {
        for (IpLayerPacketProcessor processor : list) {
            processor.output(packet);
        }
    }
}
