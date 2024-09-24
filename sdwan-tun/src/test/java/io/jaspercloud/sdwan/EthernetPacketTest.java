package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.EthernetPacket;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

public class EthernetPacketTest {

    @Test
    public void test() {
        ByteBuf byteBuf = ByteBufUtil.create();
        char[] chars = "9897cc9b6682fa7d7f5cf60008004500001c006400006301c875c0de0042c0a80d3f0800f7ff00000000".toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            byte b = (byte) Integer.parseInt(String.valueOf(chars[i]) + String.valueOf(chars[i + 1]), 16);
            byteBuf.writeByte(b);
        }
        EthernetPacket packet = EthernetPacket.decode(byteBuf);
        ByteBuf out = packet.encode();
        boolean eq = byteBuf.writerIndex() == out.writerIndex();
        System.out.println(eq);
    }
}
