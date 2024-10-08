package io.jaspercloud.sdwan.stun;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class StunEncoder extends MessageToMessageEncoder<StunPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, StunPacket msg, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        StunMessage message = msg.content();
        ByteBuf byteBuf = channel.alloc().buffer();
        ByteBuf attrsByteBuf = channel.alloc().buffer();
        try {
            for (Map.Entry<AttrType, Attr> entry : message.getAttrs().entrySet()) {
                AttrType key = entry.getKey();
                Attr value = entry.getValue();
                ByteBuf attrByteBuf = channel.alloc().buffer();
                ByteBuf valueByteBuf = channel.alloc().buffer();
                try {
                    value.write(valueByteBuf);
                    attrByteBuf.writeShort(key.getCode());
                    attrByteBuf.writeShort(valueByteBuf.readableBytes());
                    attrByteBuf.writeBytes(valueByteBuf);
                    attrsByteBuf.writeBytes(attrByteBuf);
                } finally {
                    valueByteBuf.release();
                    attrByteBuf.release();
                }
            }
            byteBuf.writeShort(message.getMessageType().getCode());
            byteBuf.writeShort(attrsByteBuf.readableBytes());
            byteBuf.writeInt(StunMessage.Cookie);
            byteBuf.writeBytes(message.getTranId().getBytes());
            byteBuf.writeBytes(attrsByteBuf);
        } finally {
            attrsByteBuf.release();
        }
        DatagramPacket datagramPacket = new DatagramPacket(byteBuf, msg.recipient());
        out.add(datagramPacket);
    }
}
