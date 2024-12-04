package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.Lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeManager implements Lifecycle {

    private Map<String, SDWanProtos.NodeInfo> nodeInfoMap = new ConcurrentHashMap<>();

    public void addNode(String vip, SDWanProtos.NodeInfo node) {
        nodeInfoMap.put(vip, node);
    }

    public void delNode(String vip) {
        nodeInfoMap.remove(vip);
    }

    public List<SDWanProtos.NodeInfo> getNodeList() {
        return Collections.unmodifiableList(new ArrayList<>(nodeInfoMap.values()));
    }

    public SDWanProtos.NodeInfo getNode(String vip) {
        return nodeInfoMap.get(vip);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {
        nodeInfoMap.clear();
    }
}
