package io.jaspercloud.sdwan.platform.rpc;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.ChannelHandler;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public abstract class RpcMessageHandler extends SimpleChannelInboundHandler<SDWanProtos.RpcMessage> {

}
