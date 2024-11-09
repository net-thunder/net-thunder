package io.jaspercloud.sdwan.tranport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantSpace {

    private List<String> stunServerList;
    private List<String> relayServerList;
    //    private Cidr ipPool;
//    private Map<String, String> fixedVipMap = new ConcurrentHashMap<>();
//    private List<SdWanServerConfig.Route> routeList = new ArrayList<>();
//    private Map<String, SDWanProtos.VNATList> vnatMap = new ConcurrentHashMap<>();
    private Map<String, Channel> bindIPMap = new ConcurrentHashMap<>();

    public List<String> getStunServerList() {
        return stunServerList;
    }

    public void setStunServerList(List<String> stunServerList) {
        this.stunServerList = stunServerList;
    }

    public List<String> getRelayServerList() {
        return relayServerList;
    }

    public void setRelayServerList(List<String> relayServerList) {
        this.relayServerList = relayServerList;
    }

    public Channel getChannel(String vip) {
        return bindIPMap.get(vip);
    }

    public void addChannel(String vip, Channel channel) {
        bindIPMap.put(vip, channel);
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bindIPMap.remove(vip);
            }
        });
    }
}
