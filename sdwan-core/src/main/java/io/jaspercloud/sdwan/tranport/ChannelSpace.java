package io.jaspercloud.sdwan.tranport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelSpace {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public List<Channel> getChannelList() {
        return Collections.unmodifiableList(new ArrayList<>(channelMap.values()));
    }

    public Channel getChannel(String vip) {
        Channel channel = channelMap.get(vip);
        return channel;
    }

    public void addChannel(String vip, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                channelMap.remove(vip);
            }
        });
        channelMap.put(vip, channel);
    }

    public int count() {
        return channelMap.size();
    }
}
