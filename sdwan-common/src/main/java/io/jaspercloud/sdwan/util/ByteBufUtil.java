package io.jaspercloud.sdwan.util;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ByteBufUtil {

    private static final ByteBufAllocator DEFAULT = new UnpooledByteBufAllocator(false);

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

    public static ByteBuf toByteBuf(ByteBuffer byteBuffer) {
        ByteBuf buffer = DEFAULT.buffer(byteBuffer.capacity());
        buffer.writeBytes(byteBuffer);
        return buffer;
    }

    public static byte[] toBytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static byte[] toBytes(String data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        char[] chars = data.toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            String s = String.valueOf(chars[i]) + String.valueOf(chars[i + 1]);
            byte b = (byte) Integer.parseInt(s, 16);
            stream.write(b);
        }
        byte[] bytes = stream.toByteArray();
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
