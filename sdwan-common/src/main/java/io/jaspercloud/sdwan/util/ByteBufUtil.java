package io.jaspercloud.sdwan.util;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.net.InetSocketAddress;

public class ByteBufUtil {

    private static final PooledByteBufAllocator DEFAULT = new PooledByteBufAllocator();

    public static ByteBuf newPacketBuf() {
        return DEFAULT.buffer(1500);
    }

    public static ByteBuf create() {
        return DEFAULT.buffer();
    }

    public static ByteBuf toByteBuf(byte[] bytes) {
        ByteBuf buffer = DEFAULT.buffer(bytes.length);
        buffer.writeBytes(bytes);
        return buffer;
    }

    public static byte[] toBytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static ByteBuf heapBuffer(byte[] bytes) {
        ByteBuf byteBuf = DEFAULT.heapBuffer();
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public static InetSocketAddress parseSocketAddress(SDWanProtos.SocketAddress address) {
        return new InetSocketAddress(address.getIp(), address.getPort());
    }

    public static SDWanProtos.SocketAddress toSocketAddress(InetSocketAddress address) {
        SDWanProtos.SocketAddress socketAddress = SDWanProtos.SocketAddress.newBuilder()
                .setIp(address.getHostString())
                .setPort(address.getPort())
                .build();
        return socketAddress;
    }
}
