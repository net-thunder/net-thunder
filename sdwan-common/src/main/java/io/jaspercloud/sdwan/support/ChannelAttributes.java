package io.jaspercloud.sdwan.support;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jasper
 * @create 2024/7/3
 */
@Getter
@Setter
public class ChannelAttributes {

    private String vip;
    private Long lastHeartTime;

    private String macAddress;
    private List<String> addressUriList;

    private ChannelAttributes() {

    }

    public static ChannelAttributes attr(Channel channel) {
        AttributeKey<ChannelAttributes> key = AttributeKey.valueOf("attributes");
        ChannelAttributes attribute = channel.attr(key).get();
        if (null == attribute) {
            attribute = new ChannelAttributes();
            channel.attr(key).set(attribute);
        }
        return attribute;
    }
}
