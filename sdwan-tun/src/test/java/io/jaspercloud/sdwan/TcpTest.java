package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

public class TcpTest {

    public static void main(String[] args) throws Exception {
        {
            //SYN
            String data = "45000028009100007f06b8e6c0de0042c0de005a";
            data += "8675049c000000e4000000005002faf0a6a30000";
            Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(toBytes(data)));
            TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
            ipv4Packet.setPayload(tcpPacket.encode(ipv4Packet, true, true));
            ByteBuf encode = ipv4Packet.encode(true, true);
            System.out.println("pass");
        }
        {
            //PUSH
            String data = "4510002e060040004006b278c0de0043c0de0042";
            data += "e4863ffbd877a6b7854ba1c1501800e56cfd0000";
            data += "746573740d0a";
            Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(toBytes(data)));
            TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
            ipv4Packet.setPayload(tcpPacket.encode(ipv4Packet, true, true));
            ByteBuf encode = ipv4Packet.encode(true, true);
            System.out.println();
        }
        System.out.println();
    }

    private static byte[] toBytes(String data) {
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
}
