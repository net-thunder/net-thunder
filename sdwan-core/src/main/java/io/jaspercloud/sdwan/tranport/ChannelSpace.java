package io.jaspercloud.sdwan.tranport;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelSpace {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Channel getChannel(String vip) {
        Channel channel = channelMap.get(vip);
        return channel;
    }

    public void addChannel(String vip, Channel channel) {
        channelMap.put(vip, channel);
    }

    public int count() {
        return channelMap.size();
    }
}
